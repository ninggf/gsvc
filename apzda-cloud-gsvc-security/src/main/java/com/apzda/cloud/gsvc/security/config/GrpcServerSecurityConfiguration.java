/*
 * Copyright (C) 2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.security.grpc.GrpcServerSecurityInterceptor;
import com.apzda.cloud.gsvc.security.grpc.SecurityAdvice;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.common.util.InterceptorOrder;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration(before = GrpcServerAutoConfiguration.class, after = GsvcSecurityAutoConfiguration.class)
@ConditionalOnClass({ GrpcServerAutoConfiguration.class, SecurityContextHolder.class, TokenManager.class })
public class GrpcServerSecurityConfiguration {

    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_SECURITY_AUTHENTICATION + 1)
    ServerInterceptor grpcServerSecurityInterceptor(TokenManager tokenManager, ObjectMapper objectMapper) {
        return new GrpcServerSecurityInterceptor(tokenManager, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    GrpcAuthenticationReader gsvcGrpcAuthenticationReader(TokenManager tokenManager) {
        return (serverCall, headers) -> null;
    }

    @Bean
    SecurityAdvice securityAdvice() {
        return new SecurityAdvice();
    }

}
