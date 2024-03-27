package com.apzda.cloud.gsvc.resolver;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;

public class NoneResolver implements ServiceNameResolver {

    @Override
    public String resolve(String serviceName, ServiceConfigProperties.Registry registry) {
        return serviceName;
    }

}
