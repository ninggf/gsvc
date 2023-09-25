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
package com.apzda.cloud.gsvc.security.grpc;

import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import io.grpc.*;
import lombok.val;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class GrpcClientSecurityInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        val context = SecurityContextHolder.getContext();
        if (context != null && context.getAuthentication() instanceof JwtAuthenticationToken authenticationToken) {
            val jwtToken = authenticationToken.getJwtToken();
            if (jwtToken != null) {
                callOptions = callOptions
                    .withCallCredentials(CallCredentialsHelper.authorizationHeader(jwtToken.getAccessToken()));
            }
        }

        return next.newCall(method, callOptions);
    }

}
