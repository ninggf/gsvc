/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.test.autoconfig;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.config.IServiceConfigure;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.exception.IExceptionHandler;
import com.apzda.cloud.gsvc.grpc.StubFactoryAdapter;
import com.apzda.cloud.gsvc.server.IServiceMethodHandler;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.function.Function;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration()
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class GsvcTestAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    IServiceCaller serviceCaller() {
        return new IServiceCaller() {
            @Override
            public <T, R> R unaryCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz) {
                throw new NotImplementedException();
            }

            @Override
            public <T, R> Flux<R> serverStreamingCall(Class<?> clazz, String method, T request, Class<T> reqClazz,
                    Class<R> resClazz) {
                throw new NotImplementedException();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    IServiceConfigure serviceConfigure() {
        return new IServiceConfigure() {
            @Override
            public String getSvcName(String cfgName) {
                throw new NotImplementedException();
            }

            @Override
            public Duration getReadTimeout(String svcName, boolean isRef) {
                throw new NotImplementedException();
            }

            @Override
            public Duration getReadTimeout(ServiceMethod method, boolean isRef) {
                throw new NotImplementedException();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    IExceptionHandler exceptionHandler() {
        return (error, request) -> {
            throw new NotImplementedException();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    IServiceMethodHandler serviceMethodHandler() {
        return new IServiceMethodHandler() {
            @Override
            public ServerResponse handleUnary(ServerRequest request, Class<?> serviceClz, String method,
                    Function<Object, Object> func) {
                throw new NotImplementedException();
            }

            @Override
            public ServerResponse handleServerStreaming(ServerRequest request, Class<?> serviceClz, String method,
                    Function<Object, Object> func) {
                throw new NotImplementedException();
            }

            @Override
            public ServerResponse handleBidStreaming(ServerRequest request, Class<?> serviceClz, String method,
                    Function<Object, Object> func) {
                throw new NotImplementedException();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    StubFactoryAdapter stubFactoryAdapter() {
        return new StubFactoryAdapter() {
            @Override
            public <T> T createAsyncStub(String serviceName, Class<T> stubType) {
                throw new NotImplementedException();
            }

            @Override
            public <T> T createBlockingStub(String serviceName, Class<T> stubType) {
                throw new NotImplementedException();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("server")
    ServerProperties serverProperties() {
        return new ServerProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    LocaleResolver localeResolver() {
        return new FixedLocaleResolver();
    }

}
