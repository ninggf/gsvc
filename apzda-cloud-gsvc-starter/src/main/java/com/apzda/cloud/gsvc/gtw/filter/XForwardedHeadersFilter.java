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
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Setter
@ConfigurationProperties("spring.cloud.gateway.x-forwarded")
public class XForwardedHeadersFilter implements HttpHeadersFilter, Ordered {

    /** Default http port. */
    public static final int HTTP_PORT = 80;

    /** Default https port. */
    public static final int HTTPS_PORT = 443;

    /** Http url scheme. */
    public static final String HTTP_SCHEME = "http";

    /** Https url scheme. */
    public static final String HTTPS_SCHEME = "https";

    /** X-Forwarded-For Header. */
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** X-Forwarded-Host Header. */
    public static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";

    /** X-Forwarded-Port Header. */
    public static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    /** X-Forwarded-Proto Header. */
    public static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

    /** X-Forwarded-Prefix Header. */
    public static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

    /** The order of the XForwardedHeadersFilter. */
    private int order = 0;

    /** If the XForwardedHeadersFilter is enabled. */
    @Getter
    private boolean enabled = true;

    /** If X-Forwarded-For is enabled. */
    @Getter
    private boolean forEnabled = true;

    /** If X-Forwarded-Host is enabled. */
    @Getter
    private boolean hostEnabled = true;

    /** If X-Forwarded-Port is enabled. */
    @Getter
    private boolean portEnabled = true;

    /** If X-Forwarded-Proto is enabled. */
    @Getter
    private boolean protoEnabled = true;

    /** If X-Forwarded-Prefix is enabled. */
    @Getter
    private boolean prefixEnabled = true;

    /** If appending X-Forwarded-For as a list is enabled. */
    @Getter
    private boolean forAppend = true;

    /** If appending X-Forwarded-Host as a list is enabled. */
    @Getter
    private boolean hostAppend = true;

    /** If appending X-Forwarded-Port as a list is enabled. */
    @Getter
    private boolean portAppend = true;

    /** If appending X-Forwarded-Proto as a list is enabled. */
    @Getter
    private boolean protoAppend = true;

    /** If appending X-Forwarded-Prefix as a list is enabled. */
    @Getter
    private boolean prefixAppend = true;

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerHttpRequest request) {

        HttpHeaders updated = new HttpHeaders();

        for (Map.Entry<String, List<String>> entry : input.entrySet()) {
            updated.addAll(entry.getKey(), entry.getValue());
        }
        val inetSocketAddress = request.getRemoteAddress();
        if (isForEnabled() && inetSocketAddress.getAddress() != null) {
            String remoteAddr = inetSocketAddress.getAddress().getHostAddress();
            write(updated, X_FORWARDED_FOR_HEADER, remoteAddr, isForAppend());
        }
        val uri = request.getURI();

        String proto = uri.getScheme();
        if (isProtoEnabled()) {
            write(updated, X_FORWARDED_PROTO_HEADER, proto, isProtoAppend());
        }

        if (isPrefixEnabled()) {
            // If the path of the url that the gw is routing to is a subset
            // (and ending part) of the url that it is routing from then the difference
            // is the prefix e.g. if request original.com/prefix/get/ is routed
            // to routedservice:8090/get then /prefix is the prefix
            // - see XForwardedHeadersFilterTests, so first get uris, then extract paths
            // and remove one from another if it's the ending part.

            Set<URI> originalUris = Collections.EMPTY_SET;
            URI requestUri = uri;

            if (originalUris != null && requestUri != null) {

                originalUris.forEach(originalUri -> {

                    if (originalUri != null && originalUri.getPath() != null) {
                        String prefix = originalUri.getPath();

                        // strip trailing slashes before checking if request path is end
                        // of original path
                        String originalUriPath = stripTrailingSlash(originalUri);
                        String requestUriPath = stripTrailingSlash(requestUri);

                        updateRequest(updated, originalUri, originalUriPath, requestUriPath);

                    }
                });
            }
        }

        if (isPortEnabled()) {
            String port = String.valueOf(uri.getPort());
            if (uri.getPort() < 0) {
                port = String.valueOf(getDefaultPort(proto));
            }
            write(updated, X_FORWARDED_PORT_HEADER, port, isPortAppend());
        }

        if (isHostEnabled()) {
            String host = toHostHeader(request);
            write(updated, X_FORWARDED_HOST_HEADER, host, isHostAppend());
        }

        return updated;
    }

    private void updateRequest(HttpHeaders updated, URI originalUri, String originalUriPath, String requestUriPath) {
        String prefix;
        if (requestUriPath != null && (originalUriPath.endsWith(requestUriPath))) {
            prefix = substringBeforeLast(originalUriPath, requestUriPath);
            if (prefix != null && prefix.length() > 0 && prefix.length() <= originalUri.getPath().length()) {
                write(updated, X_FORWARDED_PREFIX_HEADER, prefix, isPrefixAppend());
            }
        }
    }

    private static String substringBeforeLast(String str, String separator) {
        if (ObjectUtils.isEmpty(str) || ObjectUtils.isEmpty(separator)) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    private void write(HttpHeaders headers, String name, String value, boolean append) {
        if (value == null) {
            return;
        }
        if (append) {
            headers.add(name, value);
            // these headers should be treated as a single comma separated header
            List<String> values = headers.get(name);
            String delimitedValue = StringUtils.collectionToCommaDelimitedString(values);
            headers.set(name, delimitedValue);
        }
        else {
            headers.set(name, value);
        }
    }

    private int getDefaultPort(String scheme) {
        return HTTPS_SCHEME.equals(scheme) ? HTTPS_PORT : HTTP_PORT;
    }

    private boolean hasHeader(ServerHttpRequest request, String name) {
        HttpHeaders headers = request.getHeaders();
        return headers.containsKey(name) && StringUtils.hasLength(headers.getFirst(name));
    }

    private String toHostHeader(ServerHttpRequest request) {
        int port = request.getURI().getPort();
        String host = request.getURI().getHost();
        String scheme = request.getURI().getScheme();
        if (port < 0 || (port == HTTP_PORT && HTTP_SCHEME.equals(scheme))
                || (port == HTTPS_PORT && HTTPS_SCHEME.equals(scheme))) {
            return host;
        }
        else {
            return host + ":" + port;
        }
    }

    private String stripTrailingSlash(URI uri) {
        if (uri.getPath().endsWith("/")) {
            return uri.getPath().substring(0, uri.getPath().length() - 1);
        }
        else {
            return uri.getPath();
        }
    }

}
