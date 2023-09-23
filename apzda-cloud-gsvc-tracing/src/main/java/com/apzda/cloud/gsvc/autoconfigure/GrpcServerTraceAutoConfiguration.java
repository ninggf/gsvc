/*
 * Copyright (C) 2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.autoconfigure;

import brave.grpc.GrpcTracing;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.common.util.InterceptorOrder;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(GrpcCommonTraceAutoConfiguration.class)
@AutoConfigureBefore(GrpcServerAutoConfiguration.class)
@ConditionalOnClass(GrpcGlobalServerInterceptor.class)
@ConditionalOnBean(GrpcTracing.class)
public class GrpcServerTraceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerTraceAutoConfiguration.class);

    /**
     * Configures a global server interceptor that applies brave's tracing logic to the
     * requests.
     * @param grpcTracing The grpc tracing bean.
     * @return The tracing server interceptor bean.
     */
    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_TRACING_METRICS + 1)
    public ServerInterceptor globalTraceServerInterceptorConfigurer(final GrpcTracing grpcTracing) {
        log.trace("Grpc Tracing ServerInterceptor created!");
        return grpcTracing.newServerInterceptor();
    }

}
