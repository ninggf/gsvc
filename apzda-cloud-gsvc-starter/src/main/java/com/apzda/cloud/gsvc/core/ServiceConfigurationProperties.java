package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.gtw.GroupRoute;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.style.ToStringCreator;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author fengz
 */
@ConfigurationProperties("apzda.cloud")
public class ServiceConfigurationProperties {

    private final GlobalConfig config = new GlobalConfig();

    private final Map<String, ServiceConfig> service = new LinkedHashMap<>();

    private final Map<String, ServiceConfig> reference = new LinkedHashMap<>();

    public GlobalConfig getConfig() {
        return config;
    }

    public Map<String, ServiceConfig> getService() {
        return service;
    }

    public Map<String, ServiceConfig> getReference() {
        return reference;
    }

    public ServiceConfig svcConfig(String name) {
        // todo 解决配置读取不到问题.
        return service.getOrDefault(name, new ServiceConfig());
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("config", config)
            .append("service", service)
            .append("reference", reference)
            .toString();
    }

    @Data
    public static class ServiceConfig {

        public static final MethodConfig DEFAULT_MC = new MethodConfig();

        @NotNull
        private Class<?> interfaceName;

        /**
         * 南北流量路由。
         */
        private List<GroupRoute> routes = Collections.emptyList();

        /**
         * 服务在本地时，方法执行超时时间，单位Millis. 0或负值表示永不超时.
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration timeout = Duration.ZERO;

        /**
         * 文件上传超时时间，单位Millis. 0或负值表示永不超时.
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration uploadTimeout = Duration.ofMinutes(30);

        /**
         * 服务在远程时，响应超时时间，单位Millis
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration readTimeout = Duration.ZERO;

        /**
         * 服务在远程时，连接超时时间，单位Millis
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration connectTimeout = Duration.ZERO;

        private Map<String, MethodConfig> methods = new HashMap<>();

        private Set<String> filters = new HashSet<>();

    }

    @Data
    public static class MethodConfig {

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration timeout = Duration.ZERO;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration connectTimeout = Duration.ZERO;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration readTimeout = Duration.ZERO;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration uploadTimeout = Duration.ZERO;

        private Set<String> filters = new HashSet<>();

    }

    @Data
    public static class GlobalConfig {

        /**
         * 临时文件路径
         */
        private String tmpPath;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration uploadTimeout = Duration.ZERO;

        private boolean acceptLiteralFieldNames;

        private boolean properUnsignedNumberSerialization;

        private boolean serializeLongsAsString;

    }

}
