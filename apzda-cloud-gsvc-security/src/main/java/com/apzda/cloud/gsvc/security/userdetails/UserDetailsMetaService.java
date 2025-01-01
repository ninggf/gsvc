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
package com.apzda.cloud.gsvc.security.userdetails;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface UserDetailsMetaService {

    String getTenantId(@NonNull UserDetails userDetails);

    Collection<? extends GrantedAuthority> getAuthorities(@NonNull UserDetails userDetails);

    @NonNull
    default <R> Optional<R> getMetaData(@NonNull UserDetails userDetails, @NonNull String metaKey,
            @NonNull Class<R> rClass) {
        return Optional.empty();
    }

    @NonNull
    default <R> Optional<R> getMultiMetaData(@NonNull UserDetails userDetails, @NonNull String metaKey,
            @NonNull TypeReference<R> typeReference) {
        return Optional.empty();
    }

}
