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

    /**
     * 微服务接口, "apzda.cloud.reference.xxx"中无效.
     */
    private Class<?> interfaceName;

    private final Map<String, MethodConfig> methods = new LinkedHashMap<>();

    private final List<String> plugins = new ArrayList<>();

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
     * 服务在远程时，响应超时时间，单位Millis
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration writeTimeout = Duration.ZERO;

    /**
     * 服务在远程时，连接超时时间，单位Millis
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectTimeout = Duration.ZERO;

}
