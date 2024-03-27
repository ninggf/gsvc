package com.apzda.cloud.gsvc.resolver;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;

public interface ServiceNameResolver {

    String resolve(String serviceName, ServiceConfigProperties.Registry registry);

}
