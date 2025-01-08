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
package com.apzda.cloud.rocketmq.listener;

import com.apzda.cloud.rocketmq.config.RocketConfigProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import java.lang.reflect.Type;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
public class MessageListenerDelegate implements MessageListener {

    private final IMessageListener<?, ?> listener;

    private final ObjectMapper objectMapper;

    private final TypeReference<?> typeReference;

    private final Class<?> tagClz;

    public MessageListenerDelegate(@Nonnull RocketConfigProperties.TopicConfig config,
            @Nonnull ApplicationContext context) {
        val listenerName = config.getListener();
        if (StringUtils.isNotBlank(listenerName)) {
            listener = context.getBean(listenerName, IMessageListener.class);
        }
        else {
            listener = null;
        }
        Assert.notNull(listener, String.format("Listener of %s is not specified", config.getTopic()));
        val resolvableType = ResolvableType.forClass(IMessageListener.class, listener.getClass());
        val eventClz = resolvableType.getGeneric(0).resolve();
        this.tagClz = resolvableType.getGeneric(1).resolve();
        Assert.notNull(eventClz, String.format("Listener of %s is not resolvable", config.getTopic()));
        Assert.notNull(tagClz, String.format("Listener of %s is not resolvable", config.getTopic()));
        this.typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return eventClz;
            }
        };

        objectMapper = context.getBean(ObjectMapper.class);
    }

    @Override
    public ConsumeResult consume(@Nonnull MessageView messageView) {
        try {
            if (listener.onMessage(messageView, typeReference, tagClz, objectMapper)) {
                return ConsumeResult.SUCCESS;
            }
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return ConsumeResult.FAILURE;
    }

}
