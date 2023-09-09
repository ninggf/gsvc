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

    private final GlobalConfig config = new GlobalConfig();

    private final Map<String, ServiceConfig> service = new LinkedHashMap<>();

    public ServiceConfig svcConfig(String name) {
        return service.getOrDefault(name, new ServiceConfig());
    }

}
