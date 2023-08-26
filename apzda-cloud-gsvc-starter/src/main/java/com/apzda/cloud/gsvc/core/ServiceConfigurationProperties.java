package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.gtw.GroupRoute;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author fengz
 */
@ConfigurationProperties(prefix = "apzda.cloud")
@Data
public class ServiceConfigurationProperties {

    private boolean gtwEnabled;

    private GlobalConfig config = new GlobalConfig();

    private ServiceConfig service;

    private List<ServiceConfig> reference = Collections.emptyList();

    private List<GroupRoute> routes = new ArrayList<>();

    public ServiceConfig get(int index) {
        if (index == -1) {
            return service;
        }
        return reference.get(index);
    }

    @Data
    @Validated
    public static class ServiceConfig {

        public static final MethodConfig DEFAULT_MC = new MethodConfig();

        private String app;

        private String name;

        private String contextPath = "/";

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

        /**
         * 是否启动熔断器
         */
        private boolean circuitBreakerEnabled;

        @NotNull
        private Class<?> interfaceName;

        private Map<String, MethodConfig> methods = new HashMap<>();

        public String getApp() {
            // only works on remote mode
            if (!StringUtils.hasText(app)) {
                return getName();
            }
            return app;
        }

        public String getName() {
            if (!StringUtils.hasText(name)) {
                val simpleName = interfaceName.getSimpleName();
                name = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
            }
            return name;
        }

    }

    @Data
    @Validated
    public static class MethodConfig {

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration timeout = Duration.ZERO;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration connectTimeout = Duration.ZERO;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration readTimeout = Duration.ZERO;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration uploadTimeout = Duration.ZERO;

    }

    @Data
    @Validated
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
