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
package com.apzda.cloud.rocketmq.producer;

import cn.hutool.core.util.StrUtil;
import com.apzda.cloud.rocketmq.Messenger;
import com.apzda.cloud.rocketmq.RocketMail;
import com.apzda.cloud.rocketmq.config.RocketConfigProperties;
import com.apzda.cloud.rocketmq.message.IDelayMessage;
import com.apzda.cloud.rocketmq.message.IMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.util.Collections;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Getter
public abstract class RocketmqProducer<T extends IMessage<T, E>, E extends Enum<?>>
        implements ApplicationContextAware, InitializingBean {

    private final String topic;

    private final boolean transactional;

    private final boolean direct;

    private final RocketConfigProperties.ProducerConfig config;

    private ApplicationContext applicationContext;

    private ObjectMapper objectMapper;

    private Messenger messenger;

    private Messenger simpleMessenger;

    private Clock clock;

    public RocketmqProducer(@Nonnull RocketConfigProperties.ProducerConfig config) {
        this.topic = config.getTopic();
        Assert.hasText(topic, "Topic must not be empty");

        this.transactional = config.isTransaction();
        this.direct = config.isDirect() && !this.transactional;
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.objectMapper = this.applicationContext.getBean(ObjectMapper.class);
        this.messenger = this.applicationContext.getBean("transactionalMessenger", Messenger.class);
        this.simpleMessenger = this.applicationContext.getBean("simpleMessenger", Messenger.class);
        this.clock = this.applicationContext.getBean(Clock.class);
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected Map<String, String> getDefaultProperties() {
        return Collections.emptyMap();
    }

    public final void send(@Nonnull T message, @Nonnull E tag, String group, Map<String, String> properties) {
        val mail = new RocketMail();
        try {
            if (StringUtils.hasText(group)) {
                mail.setRecipients(StrUtil.format("{}:{}:{}", topic, tag, group));
            }
            else {
                mail.setRecipients(StrUtil.format("{}:{}", topic, tag));
            }

            if (message instanceof IDelayMessage delayMessage) {
                val delay = delayMessage.getDelay();
                if (delay != null && !delay.isNegative() && !delay.isZero()) {
                    mail.setPostTime(delay.toMillis() + clock.millis());
                }
            }

            if (properties == null) {
                properties = getDefaultProperties();
            }
            else if (getDefaultProperties() != null) {
                properties.putAll(getDefaultProperties());
            }
            if (properties != null && !properties.isEmpty()) {
                mail.setProperties(properties);
            }
            mail.setAsync(message.isAsync());
            mail.setContent(objectMapper.writeValueAsString(message));
            mail.setContentType(message.getClass().getCanonicalName());
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (!message.isAsync() || !transactional && direct) {
            simpleMessenger.send(mail);
        }
        else {
            mail.setTransactional(transactional);
            messenger.send(mail);
        }
    }

    public final void send(@Nonnull T message, @Nonnull E tag) {
        send(message, tag, null, null);
    }

    public final void send(@Nonnull T message, @Nonnull E tag, String group) {
        send(message, tag, group, null);
    }

    public final void send(@Nonnull T message, @Nonnull E tag, Map<String, String> properties) {
        send(message, tag, null, properties);
    }

}
