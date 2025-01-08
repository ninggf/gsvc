/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Shutdown;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Data
@ConfigurationProperties(prefix = "apzda.cloud.rocketmq")
public class RocketConfigProperties {

    private boolean enabled = true;

    private String endpoints;

    private String namespace = "";

    private String username;

    private String password;

    private int maxAttempts = 3;

    private boolean sslEnabled = false;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration requestTimeout = Duration.ofSeconds(5);

    private LimitConfig limit = new LimitConfig();

    private PostmanConfig postman = new PostmanConfig();

    private Map<String, ConsumerConfig> consumers = new HashMap<>();

    private Map<String, ProducerConfig> producers = new HashMap<>();

    @Data
    public static class ProducerConfig {

        @NotBlank
        private String topic;

        @NotBlank
        private String group;

        private boolean transaction;

        private boolean direct;

    }

    @Data
    public static class ConsumerConfig {

        @NotBlank
        private String group;

        @Min(1)
        @Max(64)
        private int threadCount = 3;

        private int maxCacheMessageCount = 1024;

        private int maxCacheMessageSizeInBytes = 67108864;

        private boolean enabled = true;

        private Shutdown shutdown = Shutdown.IMMEDIATE;

        private List<TopicConfig> topics = new ArrayList<>();

    }

    @Data
    public static class PostmanConfig {

        /**
         * 执行器的数量
         */
        private int executorCount = 1;

        /**
         * 查询间隔，默认1秒
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration period = Duration.ofSeconds(1);

        /**
         * 重试配置
         */
        private List<Duration> retries = List.of(Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(15),
                Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(3),
                Duration.ofMinutes(4), Duration.ofMinutes(5), Duration.ofMinutes(6), Duration.ofMinutes(7),
                Duration.ofMinutes(8), Duration.ofMinutes(9), Duration.ofMinutes(10), Duration.ofMinutes(20),
                Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2));

    }

    @Data
    public static class TopicConfig {

        private String topic;

        private boolean enable = true;

        private FilterConfig filter = new FilterConfig();

        /**
         * The name of a bean which is implemented
         * {@link com.apzda.cloud.rocketmq.listener.IMessageListener}.
         */
        private String listener;

    }

    @Setter
    public static class FilterConfig {

        private String expression = "*";

        private FilterExpressionType type = FilterExpressionType.TAG;

        public String getExpression() {
            return StringUtils.defaultIfBlank(expression, "*");
        }

        public FilterExpressionType getType() {
            return Optional.ofNullable(type).orElse(FilterExpressionType.TAG);
        }

    }

    @Data
    public static class LimitConfig {

        private boolean enabled = false;

        private int limit = Integer.MAX_VALUE;

        private int maxAttempts = 3;

        @DurationUnit(ChronoUnit.SECONDS)
        private Duration interval = Duration.ofSeconds(1);

    }

}
