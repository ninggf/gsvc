package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.error.GlobalGrpcExceptionAdvice;
import com.apzda.cloud.gsvc.grpc.GrpcService;
import com.apzda.cloud.gsvc.security.config.GrpcServerSecurityConfiguration;
import com.apzda.cloud.gsvc.security.grpc.HeaderMetas;
import com.google.common.collect.Lists;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorRegistry;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import net.devh.boot.grpc.server.service.GrpcServiceDefinition;
import net.devh.boot.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@AutoConfiguration
@ConditionalOnClass(GrpcServerAutoConfiguration.class)
@Import(GlobalGrpcExceptionAdvice.class)
@Slf4j
public class GrpcServerSupportConfiguration {

    @Configuration
    @ImportAutoConfiguration({ net.devh.boot.grpc.common.autoconfigure.GrpcCommonCodecAutoConfiguration.class,
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
            GrpcServerSecurityConfiguration.class })
    static class GrpcServerAutoImporter {

        @GrpcGlobalServerInterceptor
        ServerInterceptor requestIdServerInterceptor() {
            return new ServerInterceptor() {
                @Override
                public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                        Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                    val requestId = headers.get(HeaderMetas.REQUEST_ID);
                    GsvcContextHolder.setRequestId(requestId);

                    return next.startCall(call, headers);
                }
            };
        }

    }

    @Bean
    @Primary
    GrpcServiceDiscoverer gsvcGrpcServiceDiscoverer() {
        return new AnnotationGrpcServiceDiscoverer();
    }

    @Bean
    @ConditionalOnMissingBean
    GrpcServerConfigurer gsvcGrpcServerConfigurer(GatewayServiceConfigure configure) {
        return serverBuilder -> {
            val keepAliveTime = configure.getGrpcKeepAliveTime("default", false);
            val keepAliveTimeout = configure.getGrpcKeepAliveTimeout("default", false);
            log.info("GrpcServer(keepAliveTime={}, keepAliveTimeout={})", keepAliveTime, keepAliveTimeout);
            serverBuilder.keepAliveTime(keepAliveTime.getSeconds(), TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeout.getSeconds(), TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true);
        };
    }

    @Slf4j
    public static class AnnotationGrpcServiceDiscoverer implements ApplicationContextAware, GrpcServiceDiscoverer {

        private ApplicationContext applicationContext;

        @Override
        public void setApplicationContext(@NonNull final ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public Collection<GrpcServiceDefinition> findGrpcServices() {
            Collection<String> beanNames = Arrays
                .asList(this.applicationContext.getBeanNamesForAnnotation(GrpcService.class));
            List<GrpcServiceDefinition> definitions = Lists.newArrayListWithCapacity(beanNames.size());
            GlobalServerInterceptorRegistry globalServerInterceptorRegistry = applicationContext
                .getBean(GlobalServerInterceptorRegistry.class);
            for (String beanName : beanNames) {
                BindableService bindableService = this.applicationContext.getBean(beanName, BindableService.class);
                ServerServiceDefinition serviceDefinition = bindableService.bindService();
                GrpcService grpcServiceAnnotation = applicationContext.findAnnotationOnBean(beanName,
                        GrpcService.class);

                Objects.requireNonNull(grpcServiceAnnotation, "grpcServiceAnnotation: " + beanName);
                serviceDefinition = bindInterceptors(serviceDefinition, grpcServiceAnnotation,
                        globalServerInterceptorRegistry);

                definitions.add(new GrpcServiceDefinition(beanName, bindableService.getClass(), serviceDefinition));
                log.debug("Found gRPC service: " + serviceDefinition.getServiceDescriptor().getName() + ", bean: "
                        + beanName + ", class: " + bindableService.getClass().getName());
            }
            return definitions;
        }

        private ServerServiceDefinition bindInterceptors(final ServerServiceDefinition serviceDefinition,
                final GrpcService grpcServiceAnnotation,
                final GlobalServerInterceptorRegistry globalServerInterceptorRegistry) {
            final List<ServerInterceptor> interceptors = Lists.newArrayList();
            interceptors.addAll(globalServerInterceptorRegistry.getServerInterceptors());
            for (final Class<? extends ServerInterceptor> interceptorClass : grpcServiceAnnotation.interceptors()) {
                final ServerInterceptor serverInterceptor;
                if (this.applicationContext.getBeanNamesForType(interceptorClass).length > 0) {
                    serverInterceptor = this.applicationContext.getBean(interceptorClass);
                }
                else {
                    try {
                        serverInterceptor = interceptorClass.getConstructor().newInstance();
                    }
                    catch (final Exception e) {
                        throw new BeanCreationException("Failed to create interceptor instance", e);
                    }
                }
                interceptors.add(serverInterceptor);
            }
            for (final String interceptorName : grpcServiceAnnotation.interceptorNames()) {
                interceptors.add(this.applicationContext.getBean(interceptorName, ServerInterceptor.class));
            }
            if (grpcServiceAnnotation.sortInterceptors()) {
                globalServerInterceptorRegistry.sortInterceptors(interceptors);
            }
            return ServerInterceptors.interceptForward(serviceDefinition, interceptors);
        }

    }

}
