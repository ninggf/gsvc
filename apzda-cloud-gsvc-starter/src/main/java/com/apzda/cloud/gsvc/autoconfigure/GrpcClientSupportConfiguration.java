package com.apzda.cloud.gsvc.autoconfigure;

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author fengz
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnClass(GrpcClientAutoConfiguration.class)
public class GrpcClientSupportConfiguration {

    @Configuration
    @ImportAutoConfiguration({ net.devh.boot.grpc.common.autoconfigure.GrpcCommonCodecAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientMetricAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientHealthAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientSecurityAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientTraceAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcDiscoveryClientAutoConfiguration.class })
    static class GrpcClientAutoImporter {

        // todo: remove this after then official support spring boot 3.x

    }

}
