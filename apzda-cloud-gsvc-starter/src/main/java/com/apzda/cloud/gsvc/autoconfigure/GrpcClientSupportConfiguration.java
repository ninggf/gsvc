package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.grpc.DefaultGrpcChannelFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.DefaultStubFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.GrpcChannelFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.StubFactoryAdapter;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.client.stubfactory.AsyncStubFactory;
import net.devh.boot.grpc.client.stubfactory.BlockingStubFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
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
            //net.devh.boot.grpc.client.autoconfigure.GrpcClientTraceAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcDiscoveryClientAutoConfiguration.class })
    static class GrpcClientAutoImporter {

        @Bean
        @ConditionalOnMissingBean
        GrpcChannelFactoryAdapter grpcChannelFactoryAdapter(
                net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory grpcChannelFactory,
                ServiceConfigProperties properties, ApplicationContext applicationContext) {
            return new DefaultGrpcChannelFactoryAdapter(grpcChannelFactory, properties, applicationContext);
        }

        @Bean
        @ConditionalOnMissingBean
        StubFactoryAdapter stubFactoryAdapter(ApplicationContext applicationContext, AsyncStubFactory asyncStubFactory,
                BlockingStubFactory blockingStubFactory, GrpcChannelFactoryAdapter grpcChannelFactoryAdapter) {
            return new DefaultStubFactoryAdapter(asyncStubFactory, blockingStubFactory, grpcChannelFactoryAdapter,
                    applicationContext);
        }

    }

}
