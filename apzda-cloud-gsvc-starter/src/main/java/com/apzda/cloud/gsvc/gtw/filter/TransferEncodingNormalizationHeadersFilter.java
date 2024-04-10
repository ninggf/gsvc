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

package com.apzda.cloud.gsvc.gtw.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;

public class TransferEncodingNormalizationHeadersFilter implements HttpHeadersFilter, Ordered {

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerHttpRequest request) {
        String transferEncoding = input.getFirst(HttpHeaders.TRANSFER_ENCODING);
        if (transferEncoding != null && "chunked".equalsIgnoreCase(transferEncoding.trim())
                && input.containsKey(HttpHeaders.CONTENT_LENGTH)) {

            HttpHeaders filtered = new HttpHeaders();
            // avoids read only if input is read only
            filtered.addAll(input);
            filtered.remove(HttpHeaders.CONTENT_LENGTH);
            return filtered;
        }

        return input;
    }

}
