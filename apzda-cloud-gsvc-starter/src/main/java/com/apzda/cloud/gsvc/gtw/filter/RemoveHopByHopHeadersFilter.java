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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

@ConfigurationProperties("spring.cloud.gateway.filter.remove-hop-by-hop")
public class RemoveHopByHopHeadersFilter implements HttpHeadersFilter, Ordered {

    /**
     * Headers to remove as the result of applying the filter.
     */
    public static final Set<String> HEADERS_REMOVED_ON_REQUEST = new HashSet<>(
            Arrays.asList("connection", "keep-alive", "transfer-encoding", "te", "trailer", "proxy-authorization",
                    "proxy-authenticate", "x-application-context", "upgrade", "vary", "x-request-id"
            // these two are not listed in
            // https://tools.ietf.org/html/draft-ietf-httpbis-p1-messaging-14#section-7.1.3
            // "proxy-connection",
            // "content-length",
            ));

    @Setter
    private int order = Ordered.LOWEST_PRECEDENCE - 1;

    @Getter
    private Set<String> headers = HEADERS_REMOVED_ON_REQUEST;

    public void setHeaders(Set<String> headers) {
        Assert.notNull(headers, "headers may not be null");
        this.headers = headers.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public HttpHeaders filter(HttpHeaders originalHeaders, ServerHttpRequest request) {
        HttpHeaders filtered = new HttpHeaders();
        List<String> connectionOptions = originalHeaders.getConnection().stream().map(String::toLowerCase).toList();
        Set<String> headersToRemove = new HashSet<>(headers);
        headersToRemove.addAll(connectionOptions);

        for (Map.Entry<String, List<String>> entry : originalHeaders.entrySet()) {
            if (!headersToRemove.contains(entry.getKey().toLowerCase())) {
                filtered.addAll(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }

    @Override
    public boolean supports(Type type) {
        return type.equals(Type.REQUEST) || type.equals(Type.RESPONSE);
    }

}
