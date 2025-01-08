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

import cn.hutool.core.bean.BeanUtil;
import com.apzda.cloud.rocketmq.IMail;
import com.apzda.cloud.rocketmq.Messenger;
import com.apzda.cloud.rocketmq.RocketMail;
import com.apzda.cloud.rocketmq.limiter.RateLimiter;
import com.apzda.cloud.rocketmq.listener.ISendCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@RequiredArgsConstructor
@Slf4j
public class SimpleMessengerImpl implements Messenger {

    private final ClientServiceProvider clientServiceProvider;

    private final Producer producer;

    private final RateLimiter limiter;

    private final ObjectMapper objectMapper;

    private final List<ISendCallback> callbacks;

    @Override
    public void send(@Nonnull IMail mail) {
        try {
            val message = createMessage(mail, clientServiceProvider);
            if (limiter == null) {
                if (mail.isAsync()) {
                    this.producer.sendAsync(message).whenCompleteAsync((receipt, ex) -> {
                        if (ex != null) {
                            ISendCallback.onError(mail, callbacks, objectMapper, ex);
                        }
                        else if (receipt != null) {
                            ISendCallback.onSuccess(mail, callbacks, objectMapper);
                        }
                    });
                }
                else {
                    this.producer.send(message);
                }
            }
            else {
                limiter.send(producer, message, mail);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private static Message createMessage(@Nonnull IMail trans, @Nonnull ClientServiceProvider clientServiceProvider) {
        val mail = BeanUtil.copyProperties(trans, RocketMail.class);

        val builder = clientServiceProvider.newMessageBuilder()
            .setTopic(mail.getTopic())
            .setKeys(mail.getMailId())
            .setBody(mail.getContent().getBytes(StandardCharsets.UTF_8));

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
