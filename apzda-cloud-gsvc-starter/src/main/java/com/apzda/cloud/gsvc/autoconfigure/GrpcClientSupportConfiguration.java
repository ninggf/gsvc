package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.grpc.DefaultGrpcChannelFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.DefaultStubFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.GrpcChannelFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.StubFactoryAdapter;
import com.apzda.cloud.gsvc.security.config.GrpcClientSecurityConfiguration;
import com.apzda.cloud.gsvc.security.grpc.HeaderMetas;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelConfigurer;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.client.stubfactory.AsyncStubFactory;
import net.devh.boot.grpc.client.stubfactory.BlockingStubFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnClass(GrpcClientAutoConfiguration.class)
@Slf4j
public class GrpcClientSupportConfiguration {

    @Configuration
    @ImportAutoConfiguration({ net.devh.boot.grpc.common.autoconfigure.GrpcCommonCodecAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientMetricAutoConfiguration.class,
            net.devh.boot.grpc.client.autoconfigure.GrpcClientHealthAutoConfiguration.class,
            GrpcClientSecurityConfiguration.class,
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

        @Bean
        GrpcChannelConfigurer gsvcClientConfigurer(GatewayServiceConfigure configure) {
            return ((channelBuilder, cfgName) -> {
                val keepAliveTime = configure.getGrpcKeepAliveTime(cfgName, true);
                val keepAliveTimeout = configure.getGrpcKeepAliveTimeout(cfgName, true);
                log.debug("ChannelBuilder for {} Stub, keepAliveTime = {}, keepAliveTimeout = {}", cfgName,
                        keepAliveTime, keepAliveTimeout);
                channelBuilder.keepAliveTime(keepAliveTime.toSeconds(), TimeUnit.SECONDS);
                channelBuilder.keepAliveTimeout(keepAliveTimeout.toSeconds(), TimeUnit.SECONDS);
            });
        }

    }

    @GrpcGlobalClientInterceptor
    ClientInterceptor requestIdInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions, Channel next) {
                val requestId = GsvcContextHolder.getRequestId();

                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        headers.put(HeaderMetas.REQUEST_ID, requestId);
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }

}
