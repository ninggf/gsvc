package com.apzda.cloud.gsvc.autoconfigure;

import cn.hutool.core.lang.UUID;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.error.GlobalGrpcExceptionAdvice;
import com.apzda.cloud.gsvc.grpc.GrpcService;
import com.apzda.cloud.gsvc.grpc.GsvcContextServerCallListener;
import com.apzda.cloud.gsvc.security.grpc.HeaderMetas;
import com.google.common.collect.Lists;
import io.grpc.*;
import io.grpc.inprocess.InProcessSocketAddress;
import io.netty.channel.local.LocalAddress;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorRegistry;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import net.devh.boot.grpc.server.service.GrpcServiceDefinition;
import net.devh.boot.grpc.server.service.GrpcServiceDiscoverer;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@AutoConfiguration(before = GrpcServerAutoConfiguration.class)
@ConditionalOnClass(GrpcServerAutoConfiguration.class)
@Slf4j
public class GrpcServerSupportConfiguration {

    public static final Context.Key<GsvcContextHolder.GsvcContext> GSVC_CONTEXT_KEY = Context.key("gsvc-context");

    @Configuration
    static class GrpcServerAutoImporter {

        @GrpcGlobalServerInterceptor
        @Order(-1)
        ServerInterceptor requestIdServerInterceptor(Locale defaultLocale) {
            return new ServerInterceptor() {
                @Override
                public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                        Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                    val context = GsvcContextHolder.current();
                    val requestId = StringUtils.defaultIfBlank(headers.get(HeaderMetas.REQUEST_ID),
                            StringUtils.defaultIfBlank(MDC.get("traceId"), UUID.randomUUID().toString(true)));
                    context.setRequestId(requestId);

                    val language = headers.get(HeaderMetas.LANGUAGE);
                    context.setLocale(defaultLocale);
                    if (StringUtils.isNotBlank(language)) {
                        try {
                            val locale = LocaleUtils.toLocale(language);
                            context.setLocale(locale);
                        }
                        catch (Exception ignored) {
                        }
                    }

                    val serviceName = call.getMethodDescriptor().getFullMethodName();
                    context.setSvcName(serviceName);

                    val remoteIp = headers.get(HeaderMetas.REMOTE_IP);
                    val socketAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                    if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
                        context.setRemoteAddr(inetSocketAddress.getAddress().getHostAddress());
                    }
                    else if (socketAddress instanceof InProcessSocketAddress || socketAddress instanceof LocalAddress
                            || socketAddress instanceof io.grpc.netty.shaded.io.netty.channel.local.LocalAddress) {
                        context.setRemoteAddr("127.0.0.1");
                    }
                    else {
                        context.setRemoteAddr("0.0.0.0");
                    }

                    context.setRemoteIp(StringUtils.defaultIfBlank(remoteIp, context.getRemoteAddr()));
                    context.setGrpcAttributes(call.getAttributes());
                    context.setGrpcMetadata(headers);

                    val grpcContext = Context.current().withValue(GSVC_CONTEXT_KEY, context);
                    val previousContext = grpcContext.attach();

                    try {
                        return new GsvcContextServerCallListener<>(next.startCall(call, headers), grpcContext, context);
                    }
                    finally {
                        GsvcContextHolder.clear();
                        grpcContext.detach(previousContext);
                    }
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

    @Bean
    @ConditionalOnMissingBean
    GlobalGrpcExceptionAdvice globalGrpcExceptionAdvice() {
        return new GlobalGrpcExceptionAdvice();
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
