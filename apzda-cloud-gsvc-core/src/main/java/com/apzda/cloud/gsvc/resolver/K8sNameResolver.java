package com.apzda.cloud.gsvc.resolver;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.utils.StringUtils;
import lombok.val;

public class K8sNameResolver implements ServiceNameResolver {

    @Override
    public String resolve(String serviceName, ServiceConfigProperties.Registry registry) {
        val nameStyle = registry.getNameStyle();
        if (nameStyle == ServiceConfigProperties.NameStyle.CAMEL) {
            return StringUtils.lowerFirst(serviceName);
        }
        return StringUtils.toDashed(serviceName).toLowerCase();
    }

}
