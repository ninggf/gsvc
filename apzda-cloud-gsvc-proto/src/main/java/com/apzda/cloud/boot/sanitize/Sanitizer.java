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
package com.apzda.cloud.boot.sanitize;

import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.core.ResolvableType;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface Sanitizer<T> {

    T sanitize(T text, String[] configure);

    default boolean supports(@Nonnull Object value) {
        val aClass = value.getClass();
        val resolvableType = ResolvableType.forClass(Sanitizer.class, this.getClass());
        val aClass1 = resolvableType.resolveGeneric(0);

        return aClass1 != null && aClass1.isAssignableFrom(aClass);
    }

}
