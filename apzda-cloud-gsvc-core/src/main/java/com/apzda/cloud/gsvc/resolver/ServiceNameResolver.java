package com.apzda.cloud.gsvc.resolver;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;

import java.util.HashMap;
import java.util.Map;

public interface ServiceNameResolver {

    Map<ServiceConfigProperties.RegistryType, ServiceNameResolver> RESOLVERS = new HashMap<>() {
        {
            put(ServiceConfigProperties.RegistryType.NONE, new NoneResolver());
            put(ServiceConfigProperties.RegistryType.DOCKER, new DockerNameResolver());
            put(ServiceConfigProperties.RegistryType.K8S, new K8sNameResolver());
            put(ServiceConfigProperties.RegistryType.EUREKA, new EurekaNameResolver());
            put(ServiceConfigProperties.RegistryType.NACOS, new NacosNameResolver());
        }
    };

    String resolve(String serviceName, ServiceConfigProperties.Registry registry);

}
