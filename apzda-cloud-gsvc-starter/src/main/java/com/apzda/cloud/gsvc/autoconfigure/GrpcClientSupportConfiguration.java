package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.exception.ExceptionTransformer;
import com.apzda.cloud.gsvc.grpc.DefaultGrpcChannelFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.DefaultStubFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.GrpcChannelFactoryAdapter;
import com.apzda.cloud.gsvc.grpc.StubFactoryAdapter;
import com.apzda.cloud.gsvc.security.grpc.HeaderMetas;
import io.grpc.*;
import io.grpc.netty.shaded.io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelConfigurer;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.client.stubfactory.AsyncStubFactory;
import net.devh.boot.grpc.client.stubfactory.BlockingStubFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@AutoConfiguration(before = GrpcClientAutoConfiguration.class)
@EnableConfigurationProperties
@ConditionalOnClass(GrpcClientAutoConfiguration.class)
@Slf4j
public class GrpcClientSupportConfiguration {

    @Configuration
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

        @Bean
        ExceptionTransformer grpcClientExceptionTransformer() {
            return new ExceptionTransformer() {
                @Override
                public ErrorResponseException transform(Throwable exception) {
                    if (exception instanceof StatusRuntimeException se) {
                        val context = GsvcContextHolder.current();
                        val status = se.getStatus();
                        val statusCode = status.getCode();
                        val cause = status.getCause();
                        log.debug("gRPC({}) Call failed: {} - {}", context.getSvcName(), statusCode,
                                status.getDescription(), cause);
                        HttpStatusCode code;
                        ProblemDetail pd;
                        if (cause instanceof TimeoutException
                                || cause instanceof io.netty.handler.timeout.TimeoutException) {
                            pd = ProblemDetail.forStatus(504);
                            code = HttpStatus.GATEWAY_TIMEOUT;
                        }
                        else if (cause instanceof IOException) {
                            pd = ProblemDetail.forStatus(502);
                            code = HttpStatus.BAD_GATEWAY;
                        }
                        else if (statusCode == Status.UNAVAILABLE.getCode()) {
                            pd = ProblemDetail.forStatus(503);
                            code = HttpStatus.SERVICE_UNAVAILABLE;
                        }
                        else if (statusCode == Status.UNAUTHENTICATED.getCode()) {
                            pd = ProblemDetail.forStatus(401);
                            code = HttpStatus.UNAUTHORIZED;
                        }
                        else if (statusCode == Status.PERMISSION_DENIED.getCode()) {
                            pd = ProblemDetail.forStatus(403);
                            code = HttpStatus.FORBIDDEN;
                        }
                        else {
                            pd = ProblemDetail.forStatus(500);
                            code = HttpStatus.INTERNAL_SERVER_ERROR;
                        }
                        pd.setTitle(statusCode.name() + " - " + status.getDescription());
                        if (cause != null) {
                            pd.setDetail(cause.getMessage());
                        }
                        else {
                            pd.setDetail(status.getDescription());
                        }
                        return new ErrorResponseException(code, pd, exception);
                    }
                    return new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                @Override
                public boolean supports(Class<? extends Throwable> eClass) {
                    return StatusRuntimeException.class.isAssignableFrom(eClass);
                }
            };
        }

    }

    @GrpcGlobalClientInterceptor
    ClientInterceptor requestIdInterceptor(LocaleResolver localeResolver) {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions, Channel next) {
                val context = GsvcContextHolder.current();
                val serviceName = method.getServiceName();
                context.setSvcName(serviceName);
                var locale = context.getLocale();
                if (locale == null) {
                    context.setLocale(localeResolver.resolveLocale(GsvcContextHolder.getRequest().orElse(null)));
                }
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        context.restore();
                        headers.put(HeaderMetas.REQUEST_ID, context.getRequestId());
                        headers.put(HeaderMetas.LANGUAGE, context.getLocale().toLanguageTag());
                        // bookmark: ClientInterceptor
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }

}
