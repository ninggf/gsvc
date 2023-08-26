package com.apzda.cloud.gsvc.autoconfigure;

import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * @author fengz
 */
@AutoConfiguration
@ConditionalOnClass(GrpcServerAutoConfiguration.class)
public class GrpcServerSupportConfiguration {

    @Configuration
    @ImportAutoConfiguration({ net.devh.boot.grpc.common.autoconfigure.GrpcCommonCodecAutoConfiguration.class,
            net.devh.boot.grpc.common.autoconfigure.GrpcCommonTraceAutoConfiguration.class,

            net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcMetadataConsulConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcMetadataEurekaConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcMetadataNacosConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcMetadataZookeeperConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcReflectionServiceAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerTraceAutoConfiguration.class })
    static class GrpcServerAutoImporter {

        // todo: remove this after then official support spring boot 3.x

    }

}
