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
package com.apzda.cloud.gsvc.autoconfigure;

import com.alibaba.csp.sentinel.adapter.grpc.SentinelGrpcClientInterceptor;
import com.alibaba.csp.sentinel.adapter.grpc.SentinelGrpcServerInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration
@ConditionalOnClass({ ServerInterceptor.class, ClientInterceptor.class })
public class SentinelGrpcAutoConfiguration {

    @GrpcGlobalClientInterceptor
    SentinelGrpcClientInterceptor sentinelGrpcClientInterceptor() {
        return new SentinelGrpcClientInterceptor();
    }

    @GrpcGlobalServerInterceptor
    SentinelGrpcServerInterceptor sentinelGrpcServerInterceptor() {
        return new SentinelGrpcServerInterceptor();
    }

}
