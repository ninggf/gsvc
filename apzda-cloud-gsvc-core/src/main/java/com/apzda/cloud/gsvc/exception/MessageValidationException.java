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
package com.apzda.cloud.gsvc.exception;

import build.buf.validate.Violation;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.google.protobuf.Descriptors;
import lombok.Getter;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
public class MessageValidationException extends RuntimeException {

    private final List<Violation> violations;

    private final Descriptors.Descriptor descriptor;

    public MessageValidationException(List<Violation> violations, Descriptors.Descriptor descriptorForType) {
        super(ServiceError.BIND_ERROR.message);
        this.violations = violations;
        this.descriptor = descriptorForType;
    }

}
