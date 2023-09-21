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
package com.apzda.cloud.gsvc.grpc;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.google.common.collect.Lists;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class DefaultGrpcChannelFactoryAdapter implements GrpcChannelFactoryAdapter {

    private final net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory grpcChannelFactory;

    private final ServiceConfigProperties properties;

    private final ApplicationContext applicationContext;

    @Override
    public Channel createChannel(String name) {
        val serviceConfig = properties.refConfig(name);
        val grpc = serviceConfig.getGrpc();

        return grpcChannelFactory.createChannel(name, clientInterceptors(grpc.getInterceptors()),
                grpc.isSortInterceptors());
    }

    protected List<ClientInterceptor> clientInterceptors(List<String> interceptors) {
        final List<ClientInterceptor> clientInterceptors = Lists.newArrayList();
        for (String interceptor : interceptors) {
            val bean = applicationContext.getBean(interceptor, ClientInterceptor.class);
            clientInterceptors.add(bean);
        }
        return clientInterceptors;
    }

}
