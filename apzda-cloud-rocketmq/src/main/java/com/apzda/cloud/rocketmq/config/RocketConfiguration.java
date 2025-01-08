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
package com.apzda.cloud.rocketmq.config;

import com.apzda.cloud.gsvc.infra.Counter;
import com.apzda.cloud.rocketmq.Messenger;
import com.apzda.cloud.rocketmq.consumer.AbstractConsumer;
import com.apzda.cloud.rocketmq.consumer.DefaultConsumer;
import com.apzda.cloud.rocketmq.domain.service.IMailboxService;
import com.apzda.cloud.rocketmq.limiter.RateLimiter;
import com.apzda.cloud.rocketmq.limiter.RocketRateLimiter;
import com.apzda.cloud.rocketmq.listener.ISendCallback;
import com.apzda.cloud.rocketmq.postman.SimpleMessengerImpl;
import com.apzda.cloud.rocketmq.postman.TransactionalMessengerImpl;
import com.apzda.cloud.rocketmq.producer.RocketMqTransactionChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.CollectionUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Configuration(proxyBeanMethods = false)
@ComponentScan("com.apzda.cloud.rocketmq.domain")
@MapperScan("com.apzda.cloud.rocketmq.domain.mapper")
@EnableConfigurationProperties(RocketConfigProperties.class)
public class RocketConfiguration {

    @Bean
    @ConditionalOnMissingBean
    static TransactionChecker transactionChecker(IMailboxService mailboxService, ObjectMapper objectMapper,
            ObjectProvider<ISendCallback> callbackProvider) {
        return new RocketMqTransactionChecker(mailboxService, objectMapper, callbackProvider.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    static ClientConfiguration clientConfiguration(RocketConfigProperties properties) {
        val sessionCredentialsProvider = new StaticSessionCredentialsProvider(properties.getUsername(),
                properties.getPassword());
        return ClientConfiguration.newBuilder()
            .setEndpoints(properties.getEndpoints())
            .setNamespace(properties.getNamespace())
            .setRequestTimeout(properties.getRequestTimeout())
            .enableSsl(properties.isSslEnabled())
            .setCredentialProvider(sessionCredentialsProvider)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    static ClientServiceProvider clientServiceProvider() {
        return ClientServiceProvider.loadService();
    }

    @Bean("defaultRocketConsumer")
    @ConditionalOnMissingBean(name = "defaultRocketConsumer")
    @ConditionalOnProperty(name = "apzda.cloud.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnExpression("${apzda.cloud.rocketmq.consumers.default.enabled:true} && '${apzda.cloud.rocketmq.consumers.default.group:}' != ''")
    AbstractConsumer defaultRocketConsumer(ClientServiceProvider clientServiceProvider,
            ClientConfiguration clientConfiguration, RocketConfigProperties rocketConfigProperties) {
        val config = rocketConfigProperties.getConsumers().get("default");
        return new DefaultConsumer(clientServiceProvider, clientConfiguration, config);
    }

    @Primary
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(name = "producer")
    @ConditionalOnProperty(name = "apzda.cloud.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
    static Producer producer(ClientServiceProvider provider, ClientConfiguration configuration) throws ClientException {
        return provider.newProducerBuilder().setClientConfiguration(configuration).build();
    }

    @Bean(destroyMethod = "close")
    @Qualifier("TransactionalProducer")
    @ConditionalOnProperty(name = "apzda.cloud.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
    static Producer transactionalProducer(ClientServiceProvider provider, ClientConfiguration configuration,
            RocketConfigProperties properties, TransactionChecker transactionChecker) throws ClientException {

        val builder = provider.newProducerBuilder()
            .setClientConfiguration(configuration)
            .setTransactionChecker(transactionChecker)
            .setMaxAttempts(properties.getMaxAttempts());

        val topics = properties.getProducers()
            .values()
            .stream()
            .filter(RocketConfigProperties.ProducerConfig::isTransaction)
            .map(RocketConfigProperties.ProducerConfig::getTopic)
            .toList();

        if (!CollectionUtils.isEmpty(topics)) {
            builder.setTopics(topics.toArray(new String[0]));
        }
        return builder.build();
    }

    @Bean("simpleMessenger")
    @Qualifier("SimpleMessengerImpl")
    @ConditionalOnMissingBean(name = "simpleMessenger")
    Messenger messenger(ClientServiceProvider provider, ObjectMapper objectMapper,
            @Autowired(required = false) Producer producer, @Autowired(required = false) RateLimiter limiter,
            ObjectProvider<ISendCallback> sendCallback) {
        return new SimpleMessengerImpl(provider, producer, limiter, objectMapper,
                sendCallback.orderedStream().toList());
    }

    @Bean("transactionalMessenger")
    @Qualifier("TransactionalMessengerImpl")
    @ConditionalOnMissingBean(name = "transactionalMessenger")
    Messenger transactionalMessenger(RocketConfigProperties properties, ObjectMapper objectMapper,
            @Autowired(required = false) @Qualifier("TransactionalProducer") Producer producer,
            IMailboxService mailboxService, ClientServiceProvider clientServiceProvider,
            ObjectProvider<ISendCallback> sendCallback) {
        return new TransactionalMessengerImpl(properties, producer, objectMapper, mailboxService, clientServiceProvider,
                sendCallback.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(Counter.class)
    @ConditionalOnProperty(name = "apzda.cloud.rocketmq.limit.enabled", havingValue = "true")
    RateLimiter defaultRateLimiter(Counter counter, RocketConfigProperties properties, ObjectMapper objectMapper,
            ObjectProvider<ISendCallback> sendCallback) {
        return new RocketRateLimiter(counter, objectMapper, properties, sendCallback.orderedStream().toList());
    }

}
