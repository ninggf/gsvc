package com.apzda.cloud.gsvc.resolver;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.utils.StringUtils;
import lombok.val;

public class DockerNameResolver implements ServiceNameResolver {

    @Override
    public String resolve(String serviceName, ServiceConfigProperties.Registry registry) {
        val nameStyle = registry.getNameStyle();
        val port = registry.getPort();
        val schema = (registry.isSsl() ? "https" : "http") + "://";

        if (nameStyle == ServiceConfigProperties.NameStyle.CAMEL) {
            return schema + StringUtils.lowerFirst(serviceName) + ":" + port;
        }

        return schema + StringUtils.toDashed(serviceName).toLowerCase() + ":" + port;
    }

}
