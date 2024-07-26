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

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class MfaException extends AuthenticationError {
    public final static AuthenticationError UNSET = new MfaException(ServiceError.MFA_NOT_SETUP);
    public final static AuthenticationError NOT_VERIFIED = new MfaException(ServiceError.MFA_NOT_VERIFIED);

    MfaException(IServiceError error) {
        super(error);
    }
}
