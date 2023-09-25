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

import com.apzda.cloud.gsvc.security.token.TokenManager;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.common.security.SecurityConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class GrpcServerSecurityInterceptor implements ServerInterceptor {

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    private final TokenManager tokenManager;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        val accessToken = headers.get(SecurityConstants.AUTHORIZATION_HEADER);

        if (StringUtils.isNotBlank(accessToken)) {
            val requestId = headers.get(HeaderMetas.REQUEST_ID);
            val context = securityContextHolderStrategy.createEmptyContext();
            try {
                val authentication = tokenManager.restoreAuthentication(accessToken);
                if (authentication != null) {
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                    if (log.isTraceEnabled()) {
                        log.trace("[{}] Loading Context by {}", requestId, tokenManager);
                    }
                }
                else if (log.isTraceEnabled()) {
                    log.trace("[{}] Cannot Restore Authentication", requestId);
                }
            }
            catch (Exception e) {
                log.error("[{}] Cannot Restore Authentication: {}", requestId, e.getMessage());
            }
        }

        return next.startCall(call, headers);
    }

}
