package com.apzda.cloud.gsvc.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author unizwa
 */
@Data
@Accessors(chain = true)
public class ServiceConfig {

    public static final MethodConfig DEFAULT_MC = new MethodConfig();

    /**
     * 微服务名称，"apzda.cloud.service.xxx"中无效.
     */
    private String svcName;

    private GrpcConfig grpc = new GrpcConfig();

    private final Map<String, MethodConfig> methods = new LinkedHashMap<>();

    private final List<String> plugins = new ArrayList<>();

    /**
     * 服务在本地时，方法执行超时时间，单位Millis. 0或负值表示永不超时.
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration timeout = Duration.ZERO;

    /**
     * 服务在远程时，响应超时时间，单位Millis
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration readTimeout = Duration.ZERO;

    /**
     * 服务在远程时，写入超时时间，单位Millis
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration writeTimeout = Duration.ZERO;

    /**
     * 服务在远程时，连接超时时间，单位Millis
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectTimeout = Duration.ZERO;

    @Data
    public static class GrpcConfig {

        private boolean enabled;

        private List<String> interceptors = new ArrayList<>();

        private boolean sortInterceptors;

        @DurationUnit(ChronoUnit.SECONDS)
        private Duration keepAliveTime = Duration.ZERO;

        @DurationUnit(ChronoUnit.SECONDS)
        private Duration keepAliveTimeout = Duration.ZERO;

    }

}
