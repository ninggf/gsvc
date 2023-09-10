package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.gtw.GroupRoute;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    private final Map<String, ServiceConfig> reference = new LinkedHashMap<>();

    private final List<GroupRoute> routes = new ArrayList<>();

    public ServiceConfig svcConfig(String name) {
        return service.getOrDefault(name, new ServiceConfig());
    }

    public ServiceConfig refConfig(String name) {
        return reference.getOrDefault(name, new ServiceConfig());
    }

}
