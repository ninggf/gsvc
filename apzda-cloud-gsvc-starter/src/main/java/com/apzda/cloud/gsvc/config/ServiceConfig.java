package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.gtw.GroupRoute;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
public class ServiceConfig {

    public static final MethodConfig DEFAULT_MC = new MethodConfig();

    /**
     * 南北流量路由。
     */
    private final List<GroupRoute> routes = new ArrayList<>();

    private final Map<String, MethodConfig> methods = new LinkedHashMap<>();

    private final List<String> plugins = new ArrayList<>();

    private Class<?> interfaceName;

    private Class<?> fallbackClazz;

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

}
