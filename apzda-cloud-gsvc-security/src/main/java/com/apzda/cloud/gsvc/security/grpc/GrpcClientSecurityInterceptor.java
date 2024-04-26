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

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.authentication.AuthenticationDetails;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class GrpcClientSecurityInterceptor implements ClientInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        val context = SecurityContextHolder.getContext();
        val header = new HashMap<Metadata.Key<String>, String>();
        if (context != null && context.getAuthentication() instanceof JwtAuthenticationToken authenticationToken) {
            val jwtToken = authenticationToken.getJwtToken();
            if (jwtToken != null) {
                callOptions = callOptions
                    .withCallCredentials(CallCredentialsHelper.authorizationHeader(jwtToken.getAccessToken()));
                val details = authenticationToken.getDetails();
                if (details instanceof AuthenticationDetails) {
                    val generic = ((AuthenticationDetails) details).generic();
                    try {
                        header.put(HeaderMetas.AUTH_DETAILS, objectMapper.writeValueAsString(generic));
                    }
                    catch (Exception ignored) {
                    }
                }
            }
        }
        val gContext = GsvcContextHolder.getContext();
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                gContext.restore();
                for (val h : header.entrySet()) {
                    if (h.getValue() != null) {
                        headers.put(h.getKey(), h.getValue());
                        log.trace("Transit Header: {}", h.getKey().name());
                    }
                    else {
                        log.warn("Value of Header: {} is null, skipped!", h.getKey().name());
                    }
                }
                super.start(responseListener, headers);
            }
        };
    }

}
