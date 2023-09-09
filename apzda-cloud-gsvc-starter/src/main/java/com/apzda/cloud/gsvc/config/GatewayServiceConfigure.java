package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.core.ServiceConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.time.Duration;

/**
 * @author ninggf
 */
@RequiredArgsConstructor
public class GatewayServiceConfigure {

    private final ServiceConfigurationProperties serviceConfig;

    public ServiceConfigurationProperties.ServiceConfig getServiceConfig(String serviceName) {
        // todo 解决配置读取不到问题.
        return serviceConfig.svcConfig(serviceName);
    }

    public Duration getReadTimeout(String serviceName, String methodName) {
        val config = serviceConfig.svcConfig(serviceName);
        // 方法级
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            val mrTimeout = methodConfig.getReadTimeout();
            if (!mrTimeout.isZero()) {
                return mrTimeout;
            }
        }
        // service global
        val readTimeout = config.getReadTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        return Duration.ofSeconds(3600);
    }

    public Duration getConnectTimeout(String serviceName, String methodName) {
        val config = serviceConfig.svcConfig(serviceName);
        // method
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            val mrTimeout = methodConfig.getConnectTimeout();
            if (!mrTimeout.isZero()) {
                return mrTimeout;
            }
        }
        // service global
        val readTimeout = config.getConnectTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        return Duration.ofSeconds(5);
    }

    public Duration getTimeout(String serviceName, String methodName) {
        val config = serviceConfig.svcConfig(serviceName);
        // method
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            val mrTimeout = methodConfig.getTimeout();
            if (!mrTimeout.isZero()) {
                return mrTimeout;
            }
        }
        // service global
        val readTimeout = config.getTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        return Duration.ofSeconds(3610);
    }

    public Duration getUploadTimeout(String serviceName, String methodName) {
        val config = serviceConfig.svcConfig(serviceName);
        // method
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            val mrTimeout = methodConfig.getUploadTimeout();
            if (!mrTimeout.isZero()) {
                return mrTimeout;
            }
        }
        // service global
        var uploadTimeout = config.getUploadTimeout();
        if (!uploadTimeout.isZero()) {
            return uploadTimeout;
        }
        // gsvc global
        uploadTimeout = serviceConfig.getConfig().getUploadTimeout();
        if (!uploadTimeout.isZero()) {
            return uploadTimeout;
        }
        // default
        return Duration.ofMinutes(5);
    }

}
