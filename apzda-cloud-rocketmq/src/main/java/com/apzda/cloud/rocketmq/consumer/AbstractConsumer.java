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
package com.apzda.cloud.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.apzda.cloud.rocketmq.config.RocketConfigProperties;
import com.apzda.cloud.rocketmq.listener.MessageListenerDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.context.ApplicationListener;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Slf4j
public abstract class AbstractConsumer implements ApplicationListener<ApplicationReadyEvent>, MessageListener {

    private final List<RocketConfigProperties.TopicConfig> topics;

    private final String consumerGroup;

    private final int threadCount;

    private final int maxCacheMessageCount;

    private final boolean enabled;

    private final RocketConfigProperties.ConsumerConfig config;

    private final Map<String, FilterExpression> filters = new HashMap<>();

    private final Map<String, MessageListenerDelegate> listeners = new HashMap<>();

    protected final ClientServiceProvider clientServiceProvider;

    protected final ClientConfiguration clientConfiguration;

    private PushConsumer consumer;

    public AbstractConsumer(@Nonnull ClientServiceProvider clientServiceProvider,
            @Nonnull ClientConfiguration clientConfiguration, @Nonnull RocketConfigProperties.ConsumerConfig config) {
        this.clientServiceProvider = clientServiceProvider;
        this.clientConfiguration = clientConfiguration;
        this.consumerGroup = config.getGroup();
        this.threadCount = config.getThreadCount();
        this.maxCacheMessageCount = config.getMaxCacheMessageCount();
        this.topics = config.getTopics();
        this.enabled = config.isEnabled() && !CollectionUtils.isEmpty(topics)
                && topics.stream().anyMatch(RocketConfigProperties.TopicConfig::isEnable);
        this.config = config;
    }

    @PreDestroy
    public void close() {
        val consumerName = this.getClass().getSimpleName();
        if (this.consumer != null) {
            log.debug("{} is closing ...", consumerName);
            try {
                if (!CollectionUtils.isEmpty(this.topics)) {
                    for (RocketConfigProperties.TopicConfig topic : this.topics) {
                        try {
                            if (topic.isEnable()) {
                                this.consumer.unsubscribe(topic.getTopic());
                            }
                        }
                        catch (Exception ignored) {
                            log.warn("{} unsubscribe {} failed.", consumerName, topic.getTopic());
                        }
                    }
                    if (config.getShutdown() == Shutdown.GRACEFUL) {
                        this.consumer.close();
                    }
                }
                log.debug("{} closed successfully!", consumerName);
            }
            catch (Exception e) {
                log.error("{} Shutdown completed", consumerName, e);
            }
        }
    }

    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        if (!enabled) {
            log.warn("{}@{} is disabled", this.getClass().getSimpleName(), consumerGroup);
            return;
        }
        try {
            val topicNames = new ArrayList<String>();
            for (val topic : this.topics) {
                if (!topic.isEnable()) {
                    continue;
                }
                val topicName = topic.getTopic();
                topicNames.add(topicName);
                val filter = topic.getFilter();
                val filterExpression = new FilterExpression(filter.getExpression(), filter.getType());
                filters.put(topicName, filterExpression);
                listeners.put(topicName, new MessageListenerDelegate(topic, event.getApplicationContext()));
            }
            this.init();
            log.info("{}@{} will consume messages from topics: {}", this.getClass().getSimpleName(), this.consumerGroup,
                    topicNames);
        }
        catch (ClientException e) {
            throw new RuntimeException(StrUtil.format("Initialize {}@{} failed: {}", this.getClass().getSimpleName(),
                    this.consumerGroup, e.getMessage()), e);
        }
    }

    @Override
    public ConsumeResult consume(@Nonnull MessageView messageView) {
        val topic = messageView.getTopic();
        if (log.isTraceEnabled()) {
            val tag = messageView.getTag();
            val res = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
            log.trace("Receive Message: {} ,Topic: {}, Tag: {}, Message: {}", messageView.getKeys(), topic,
                    tag.orElse(""), res);
        }
        val listener = listeners.get(topic);
        if (listener == null) {
            throw new IllegalStateException(String.format("Consumer [%s] has no listener", topic));
        }
        return listener.consume(messageView);
    }

    protected void init() throws ClientException {
        this.consumer = this.clientServiceProvider.newPushConsumerBuilder()
            .setClientConfiguration(this.clientConfiguration)
            .setConsumerGroup(this.consumerGroup)
            .setConsumptionThreadCount(threadCount)
            .setMaxCacheMessageCount(maxCacheMessageCount)
            .setSubscriptionExpressions(filters)
            .setMessageListener(this)
            .build();
    }

}
