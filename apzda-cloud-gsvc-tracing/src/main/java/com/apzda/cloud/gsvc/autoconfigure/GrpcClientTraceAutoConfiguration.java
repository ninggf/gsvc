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
import io.grpc.ClientInterceptor;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.common.util.InterceptorOrder;
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
@AutoConfigureBefore(GrpcClientAutoConfiguration.class)
@ConditionalOnClass(GrpcGlobalClientInterceptor.class)
@ConditionalOnBean(GrpcTracing.class)
public class GrpcClientTraceAutoConfiguration {

    /**
     * Configures a global client interceptor that applies brave's tracing logic to the
     * requests.
     * @param grpcTracing The grpc tracing bean.
     * @return The tracing client interceptor bean.
     */
    @GrpcGlobalClientInterceptor
    @Order(InterceptorOrder.ORDER_TRACING_METRICS + 1)
    ClientInterceptor globalTraceClientInterceptorConfigurer(final GrpcTracing grpcTracing) {
        return grpcTracing.newClientInterceptor();
    }

}
