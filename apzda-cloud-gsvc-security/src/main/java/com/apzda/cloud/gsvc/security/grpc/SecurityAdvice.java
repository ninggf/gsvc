/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.security.grpc;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.exception.AuthenticationError;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@GrpcAdvice
@Slf4j
public class SecurityAdvice {

    @GrpcExceptionHandler(AuthenticationError.class)
    public Status handleException(AuthenticationError e) {
        val context = GsvcContextHolder.getContext();
        log.trace("gRPC({}) error: {}", context.getSvcName(), e.getMessage());
        return Status.PERMISSION_DENIED.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(AuthenticationException.class)
    public Status handleException(AuthenticationException e) {
        val context = GsvcContextHolder.getContext();
        log.trace("gRPC({}) error: {}", context.getSvcName(), e.getMessage());
        return Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(AccessDeniedException.class)
    public Status handleException(AccessDeniedException e) {
        val context = GsvcContextHolder.getContext();
        log.trace("gRPC({}) error: {}", context.getSvcName(), e.getMessage());
        return Status.PERMISSION_DENIED.withDescription(e.getMessage()).withCause(e);
    }

}
