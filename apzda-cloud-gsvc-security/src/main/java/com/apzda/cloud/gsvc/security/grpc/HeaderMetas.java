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

import io.grpc.Metadata;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class HeaderMetas {

    public static final Metadata.Key<String> REQUEST_ID = Metadata.Key.of("X-Request-Id",
            Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> LANGUAGE = Metadata.Key.of("Accept-Language",
            Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> AUTH_DETAILS = Metadata.Key.of("X-Auth-Details",
            Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> REMOTE_IP = Metadata.Key.of("X-Real-IP", Metadata.ASCII_STRING_MARSHALLER);

}
