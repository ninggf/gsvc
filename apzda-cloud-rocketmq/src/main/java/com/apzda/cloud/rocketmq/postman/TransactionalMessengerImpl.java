/*
 * Copyright (C) 2023-2025 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.rocketmq.postman;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.apzda.cloud.gsvc.exception.StopRetryException;
import com.apzda.cloud.rocketmq.IMail;
import com.apzda.cloud.rocketmq.Messenger;
import com.apzda.cloud.rocketmq.RocketMail;
import com.apzda.cloud.rocketmq.config.RocketConfigProperties;
import com.apzda.cloud.rocketmq.domain.entity.Mailbox;
import com.apzda.cloud.rocketmq.domain.service.IMailboxService;
import com.apzda.cloud.rocketmq.domain.vo.MailStatus;
import com.apzda.cloud.rocketmq.listener.ISendCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.apis.producer.Transaction;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Slf4j
public class TransactionalMessengerImpl implements Messenger, ApplicationListener<ApplicationReadyEvent> {

    private final Producer producer;

    private final ObjectMapper objectMapper;

    private final IMailboxService mailboxService;

    private final RocketConfigProperties properties;

    private final ClientServiceProvider clientServiceProvider;

    private final ScheduledThreadPoolExecutor executor;

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    private final List<ISendCallback> callbacks;

    private final int executorCount;

    private volatile boolean running = true;

    public TransactionalMessengerImpl(RocketConfigProperties properties, Producer producer, ObjectMapper objectMapper,
            IMailboxService mailboxService, ClientServiceProvider clientServiceProvider,
            List<ISendCallback> callbacks) {
        this.properties = properties;
        this.mailboxService = mailboxService;
        this.producer = producer;
        this.objectMapper = objectMapper;
        this.clientServiceProvider = clientServiceProvider;
        this.callbacks = callbacks;
        val count = properties.getPostman().getExecutorCount();
        val cpuCount = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
        this.executorCount = count < 1 ? cpuCount : count;
        this.executor = new ScheduledThreadPoolExecutor(executorCount + cpuCount, r -> {
            val thread = new Thread(r);
            thread.setName("postman-" + atomicInteger.getAndAdd(1));
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        if (this.producer == null) {
            return;
        }
        val period = properties.getPostman().getPeriod().toSeconds();
        for (int i = 0; i < executorCount; i++) {
            executor.scheduleWithFixedDelay(createMailSender(null), 0, period > 0 ? period : 1, TimeUnit.SECONDS);
        }

        log.info("Postman executor initialized: count={}, period={}s", executorCount, period);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void send(@Nonnull IMail mail) {
        val content = mail.getContent();
        Assert.hasText(content, "content must not be null");
        val recipients = mail.getRecipients();
        Assert.hasText(content, "recipients must not be null");

        val mailbox = new Mailbox();
        mailbox.setMailId(mail.getMailId());
        mailbox.setContent(content);
        mailbox.setContentType(mail.getContentType());
        mailbox.setNextRetryAt(System.currentTimeMillis());
        mailbox.setTransactional(mail.getTransactional());
        mailbox.setRetries(0);
        mailbox.setPostTime(mail.getPostTime());
        mailbox.setRecipients(recipients);
        mailbox.setProperties(mail.getProperties());
        mailbox.setStatus(MailStatus.PENDING);

        if (!mailboxService.save(mailbox)) {
            throw new RuntimeException("The mail cannot save into mailbox: " + mail);
        }
        else if (log.isDebugEnabled()) {
            log.debug("Mailbox {} queued successfully!", mail.getMailId());
        }

        if (!Boolean.TRUE.equals(mail.getTransactional())) {
            try {
                executor.submit(createMailSender(mailbox));
            }
            catch (Exception e) {
                // 理论上到不了这里。就算到了，也有兜底。
                log.warn("Failed to send({}) mail immediately: {}", mail.getRecipients(), e.getMessage());
            }
        }
    }

    @PreDestroy
    public void stop() {
        try {
            running = false;
            executor.shutdown();
            if (executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.info("Shutdown Postman executor successfully!");
            }
            else {
                log.warn("Shutdown Postman executor timeout: 30s");
            }
        }
        catch (Exception e) {
            log.warn("Cannot shutdown Postman executor: {}", e.getMessage());
        }
    }

    @Nonnull
    private MailSender createMailSender(@Nullable Mailbox mail) {
        return new MailSender(producer, mailboxService, properties, clientServiceProvider, mail, this);
    }

    @Slf4j
    private record MailSender(Producer producer, IMailboxService mailboxService, RocketConfigProperties postmanConfig,
            ClientServiceProvider clientServiceProvider, Mailbox mailbox,
            TransactionalMessengerImpl messenger) implements Runnable {

        @Override
        public void run() {
            try {
                if (mailbox != null) {
                    log.debug("Send mail immediately: {}", mailbox);
                    post(mailbox);
                }
                else {
                    Mailbox trans;
                    do {
                        trans = mailboxService.getByStatusAndNextRetryAtLe(MailStatus.PENDING,
                                System.currentTimeMillis());
                        if (trans != null) {
                            post(trans);
                        }
                    }
                    while (messenger.running && trans != null);
                }
            }
            catch (Exception e) {
                log.warn("MailSender error: {}", e.getMessage());
            }
        }

        public void post(@Nonnull Mailbox trans) {
            try {
                trans.setStatus(MailStatus.SENDING);
                if (!mailboxService.updateStatus(trans, MailStatus.PENDING)) {
                    return;
                }
                val content = trans.getContent();
                if (!StringUtils.hasText(content)) {
                    throw new StopRetryException("Content must not be null");
                }
                if (!StringUtils.hasText(trans.getRecipients())) {
                    throw new StopRetryException("Recipients must not be null");
                }
                val message = createMessage(trans, clientServiceProvider);
                final Transaction transaction;
                final SendReceipt result;
                if (Boolean.TRUE.equals(trans.getTransactional())) {
                    transaction = this.producer.beginTransaction();
                    Assert.notNull(transaction, "Transaction must not be null");
                    result = this.producer.send(message, transaction);
                }
                else {
                    transaction = null;
                    result = this.producer.send(message);
                }

                if (result == null) {
                    if (transaction != null) {
                        transaction.rollback();
                    }
                    throw new RuntimeException("Can't send mail: " + trans.getContent());
                }

                trans.setMsgId(result.getMessageId().toString());
                trans.setStatus(MailStatus.SENT);
                if (!mailboxService.updateStatus(trans, MailStatus.SENDING)) {
                    if (transaction != null) {
                        transaction.rollback();
                    }
                    throw new RuntimeException("Can't update mail status: " + trans.getContent());
                }
                try {
                    if (transaction != null) {
                        transaction.commit();
                    }
                    log.debug("Mail({}) sent successfully", trans.getContent());
                    try {
                        mailboxService.removeById(trans);
                    }
                    catch (Exception e) {
                        log.warn("Failed to remove mail: {}, wait for checker callback: {}", trans.getContent(),
                                e.getMessage());
                    }
                    if (!CollectionUtils.isEmpty(messenger.callbacks)) {
                        ISendCallback.onSuccess(trans, messenger.callbacks, messenger.objectMapper);
                    }
                }
                catch (Exception e) {
                    log.warn("Failed to commit transaction of mail({}), wait for checker callback: {}",
                            trans.getContent(), e.getMessage());
                }
            }
            catch (Exception e) {
                try {
                    val errMsg = ExceptionUtil.getSimpleMessage(ExceptionUtil.getRootCause(e));
                    val retries = postmanConfig.getPostman().getRetries();
                    val currentRetry = trans.getRetries();
                    if (!(e instanceof StopRetryException) && retries.size() >= (currentRetry + 1)) {
                        trans.setStatus(MailStatus.PENDING);
                        trans.setRetries(currentRetry + 1);
                        val duration = retries.get(currentRetry);
                        trans.setNextRetryAt(System.currentTimeMillis() + duration.toMillis());
                        log.warn("Cannot send mail and will retry in {}s: {} - {}", duration.toSeconds(),
                                trans.getContent(), errMsg);
                    }
                    else {
                        trans.setStatus(MailStatus.FAIL);
                        log.warn("Cannot send mail and stop retry: {} - {}", trans.getContent(), errMsg);
                    }
                    trans.setRemark(errMsg);
                    mailboxService.updateStatus(trans, MailStatus.SENDING);
                }
                catch (Exception e2) {
                    log.error("Cannot send mail and retry: {}", e2.getMessage(), e2);
                }

                if (!CollectionUtils.isEmpty(messenger.callbacks)) {
                    ISendCallback.onError(trans, messenger.callbacks, messenger.objectMapper, e);
                }
            }
        }
    }

    @Nonnull
    private static Message createMessage(@Nonnull Mailbox trans, @Nonnull ClientServiceProvider clientServiceProvider) {
        val mail = new RocketMail();
        mail.setMailId(trans.getMailId());
        mail.setRecipients(trans.getRecipients());
        mail.setContent(trans.getContent());
        mail.setPostTime(trans.getPostTime());
        mail.setProperties(trans.getProperties());

        val builder = clientServiceProvider.newMessageBuilder()
            .setTopic(mail.getTopic())
            .setKeys(trans.getMailId())
            .setBody(trans.getContent().getBytes(StandardCharsets.UTF_8));

        if (StringUtils.hasText(mail.getTags())) {
            builder.setTag(mail.getTags());
        }
        if (StringUtils.hasText(mail.getGroup())) {
            builder.setMessageGroup(mail.getGroup());
        }
        if (mail.getPostTime() != null) {
            builder.setDeliveryTimestamp(mail.getPostTime());
        }
        if (!CollectionUtils.isEmpty(mail.getProperties())) {
            mail.getProperties().forEach(builder::addProperty);
        }
        return builder.build();
    }

}
