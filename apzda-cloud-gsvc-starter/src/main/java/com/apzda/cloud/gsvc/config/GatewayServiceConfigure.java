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

    public ServiceConfigurationProperties.ServiceConfig getServiceConfig(int index) {
        return serviceConfig.get(index);
    }

    public Duration getReadTimeout(int index, String methodName) {
        val config = serviceConfig.get(index);
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

    public Duration getConnectTimeout(int index, String methodName) {
        val config = serviceConfig.get(index);
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

    public Duration getTimeout(int index, String methodName) {
        val config = serviceConfig.get(index);
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

    public Duration getUploadTimeout(int index, String methodName) {
        val config = serviceConfig.get(index);
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
