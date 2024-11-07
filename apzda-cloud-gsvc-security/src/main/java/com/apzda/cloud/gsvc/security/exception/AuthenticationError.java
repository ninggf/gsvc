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
package com.apzda.cloud.gsvc.security.exception;

import com.apzda.cloud.gsvc.IServiceError;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.NoStackLogError;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
public class AuthenticationError extends AuthenticationException implements NoStackLogError {

    protected final IServiceError error;

    public AuthenticationError(@Nonnull IServiceError error) {
        super(error.message());
        this.error = error;
    }

    public AuthenticationError() {
        super(ServiceError.UNAUTHORIZED.message());
        this.error = ServiceError.UNAUTHORIZED;
    }

}
