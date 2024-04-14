/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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

import com.alibaba.csp.sentinel.adapter.grpc.SentinelGrpcServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(GrpcGlobalServerInterceptor.class)
@Slf4j
public class SentinelGrpcServerConfiguration {

    @GrpcGlobalServerInterceptor
    SentinelGrpcServerInterceptor sentinelGrpcServerInterceptor() {
        log.trace("SentinelGrpcServerInterceptor installed");
        return new SentinelGrpcServerInterceptor();
    }

}
