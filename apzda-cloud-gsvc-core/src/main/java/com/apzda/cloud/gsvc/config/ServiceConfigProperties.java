package com.apzda.cloud.gsvc.config;

import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author fengz
 */
@Getter
@ToString
@ConfigurationProperties("apzda.cloud")
public class ServiceConfigProperties {

    private final static ServiceConfig SERVICE_DEFAULT = new ServiceConfig();

    private final static ServiceConfig REFERENCE_DEFAULT = new ServiceConfig();

    private final GlobalConfig config = new GlobalConfig();

    private final Map<String, ServiceConfig> service = new LinkedHashMap<>();

    private final Map<String, ServiceConfig> reference = new LinkedHashMap<>();

    private final Map<String, GatewayRouteConfig> gateway = new LinkedHashMap<>();

    public ServiceConfig svcConfig(String name) {
        return service.getOrDefault(name, SERVICE_DEFAULT);
    }

    public ServiceConfig refConfig(String name) {
        return reference.getOrDefault(name, REFERENCE_DEFAULT);
    }

}
