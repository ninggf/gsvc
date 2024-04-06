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
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.common.security.SecurityConstants;
import net.devh.boot.grpc.server.security.interceptors.AbstractAuthenticatingServerCallListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import static net.devh.boot.grpc.server.security.interceptors.AuthenticatingServerInterceptor.AUTHENTICATION_CONTEXT_KEY;
import static net.devh.boot.grpc.server.security.interceptors.AuthenticatingServerInterceptor.SECURITY_CONTEXT_KEY;

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
                    log.debug("[{}] Authentication successful: {} ({})", requestId, authentication.getName(),
                            authentication.getAuthorities());
                    @SuppressWarnings("deprecation")
                    val grpcContext = Context.current()
                        .withValues(SECURITY_CONTEXT_KEY, context, AUTHENTICATION_CONTEXT_KEY, authentication);
                    val previousContext = grpcContext.attach();

                    try {
                        return new AuthenticatingServerCallListener<>(next.startCall(call, headers), grpcContext,
                                context);
                    }
                    finally {
                        SecurityContextHolder.clearContext();
                        grpcContext.detach(previousContext);
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

    private static class AuthenticatingServerCallListener<ReqT> extends AbstractAuthenticatingServerCallListener<ReqT> {

        private final SecurityContext securityContext;

        public AuthenticatingServerCallListener(final ServerCall.Listener<ReqT> delegate, final Context grpcContext,
                final SecurityContext securityContext) {
            super(delegate, grpcContext);
            this.securityContext = securityContext;
        }

        @Override
        protected void attachAuthenticationContext() {
            SecurityContextHolder.setContext(this.securityContext);
        }

        @Override
        protected void detachAuthenticationContext() {
            SecurityContextHolder.clearContext();
        }

    }

}
