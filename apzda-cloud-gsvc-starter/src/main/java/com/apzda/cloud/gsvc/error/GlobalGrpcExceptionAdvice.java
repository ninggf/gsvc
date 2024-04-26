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
package com.apzda.cloud.gsvc.error;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.exception.NoStackLogError;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@GrpcAdvice
@Slf4j
public class GlobalGrpcExceptionAdvice {

    @GrpcExceptionHandler
    public Status handleException(Exception e) {
        val context = GsvcContextHolder.getContext();
        if (e instanceof NoStackLogError) {
            log.error("gRPC({}) error: {}", context.getSvcName(), e.getMessage());
        }
        else {
            log.error("gRPC({}) error: {}", context.getSvcName(), e.getMessage(), e);
        }
        return Status.INTERNAL.withDescription(e.getMessage()).withCause(e);
    }

}
