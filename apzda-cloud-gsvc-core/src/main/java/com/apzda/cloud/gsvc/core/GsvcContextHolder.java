package com.apzda.cloud.gsvc.core;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.val;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author ninggf
 */
public class GsvcContextHolder {

    private static final XForwardedHeadersFilter X_FORWARDED_HEADERS_FILTER = new XForwardedHeadersFilter();

    private static final String FILTERED_HTTP_HEADERS = "FILTERED_HTTP_HEADERS";

    private static final String HTTP_COOKIES = "FILTERED_HTTP_COOKIES";

    private static final ThreadLocal<String> CONTEXT_BOX = new InheritableThreadLocal<>();

    public static Optional<HttpServletRequest> getRequest() {
        val requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes request) {
            return Optional.of(request.getRequest());
        }
        return Optional.empty();
    }

    public static Optional<HttpServletResponse> getResponse() {
        val requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes request) {
            return Optional.ofNullable(request.getResponse());
        }
        return Optional.empty();
    }

    public static DefaultHttpHeaders headers() {
        val request = getRequest();
        if (request.isPresent()) {
            HttpServletRequest httpServletRequest = request.get();
            Object filtered = httpServletRequest.getAttribute(FILTERED_HTTP_HEADERS);
            if (filtered == null) {
                synchronized (httpServletRequest) {
                    filtered = httpServletRequest.getAttribute(FILTERED_HTTP_HEADERS);
                    if (filtered == null) {
                        val httpHeaders = HttpHeaders.readOnlyHttpHeaders(createDefaultHttpHeaders(httpServletRequest));
                        HttpHeaders filtered1 = X_FORWARDED_HEADERS_FILTER.filter(httpHeaders, httpServletRequest);
                        val defaultHttpHeaders = new DefaultHttpHeaders();
                        filtered1.forEach(defaultHttpHeaders::set);
                        httpServletRequest.setAttribute(FILTERED_HTTP_HEADERS, defaultHttpHeaders);
                        filtered = defaultHttpHeaders;
                    }
                }
            }
            if (filtered instanceof DefaultHttpHeaders defaultHttpHeaders) {
                return defaultHttpHeaders;
            }
        }
        return new DefaultHttpHeaders();
    }

    public static String header(String name) {
        return headers().get(name);
    }

    public static Map<String, String> headers(String prefix) {
        val headers = new HashMap<String, String>();
        headers().forEach(header -> {
            if (StringUtils.startsWithIgnoreCase(header.getKey(), prefix)) {
                headers.put(header.getKey(), header.getValue());
            }
        });
        return headers;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, HttpCookie> cookies() {
        val request = getRequest();
        if (request.isPresent()) {
            final HttpServletRequest httpServletRequest = request.get();
            Object filtered = httpServletRequest.getAttribute(HTTP_COOKIES);
            if (filtered != null) {
                return (Map<String, HttpCookie>) filtered;
            }
            synchronized (httpServletRequest) {
                filtered = httpServletRequest.getAttribute(HTTP_COOKIES);
                if (filtered != null) {
                    return (Map<String, HttpCookie>) filtered;
                }
                val cookies = new HashMap<String, HttpCookie>();
                val cookies1 = httpServletRequest.getCookies();
                if (cookies1 != null) {
                    for (Cookie cookie : cookies1) {
                        cookies.put(cookie.getName(), new HttpCookie(cookie.getName(), cookie.getValue()));
                    }
                }
                httpServletRequest.setAttribute(HTTP_COOKIES, cookies);
                return cookies;
            }
        }
        return Collections.emptyMap();
    }

    public static Map<String, HttpCookie> cookies(String prefix) {
        val cookies = new HashMap<String, HttpCookie>();
        cookies().forEach(((s, httpCookie) -> {
            if (StringUtils.startsWithIgnoreCase(s, prefix)) {
                cookies.put(s, httpCookie);
            }
        }));
        return cookies;
    }

    private static MultiValueMap<String, String> createDefaultHttpHeaders(HttpServletRequest request) {
        final MultiValueMap<String, String> headers = CollectionUtils
            .toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH));
        for (Enumeration<?> names = request.getHeaderNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            for (Enumeration<?> values = request.getHeaders(name); values.hasMoreElements();) {
                headers.add(name, (String) values.nextElement());
            }
        }
        if (!headers.containsKey("x-request-id")) {
            val rid = request.getAttribute("X-Request-Id");
            if (rid != null) {
                headers.add("X-Request-Id", (String) rid);
            }
            else {
                headers.add("X-Request-Id", UUID.randomUUID().toString());
                request.setAttribute("X-Request-Id", headers.getFirst("X-Request-Id"));
            }
        }
        return headers;
    }

    public static String getRequestId() {
        val requestId = header("x-request-id");
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        return org.apache.commons.lang3.StringUtils.defaultString(CONTEXT_BOX.get(), "");
    }

    public static void setRequestId(String requestId) {
        CONTEXT_BOX.set(requestId);
    }

    @Data
    static class XForwardedHeadersFilter {

        /**
         * Default http port.
         */
        public static final int HTTP_PORT = 80;

        /**
         * Default https port.
         */
        public static final int HTTPS_PORT = 443;

        /**
         * Http url scheme.
         */
        public static final String HTTP_SCHEME = "http";

        /**
         * Https url scheme.
         */
        public static final String HTTPS_SCHEME = "https";

        /**
         * X-Forwarded-For Header.
         */
        public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

        /**
         * X-Forwarded-Host Header.
         */
        public static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";

        /**
         * X-Forwarded-Port Header.
         */
        public static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

        /**
         * X-Forwarded-Proto Header.
         */
        public static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

        /**
         * X-Forwarded-Prefix Header.
         */
        public static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

        /**
         * The order of the XForwardedHeadersFilter.
         */
        private int order = 0;

        /**
         * If the XForwardedHeadersFilter is enabled.
         */
        private boolean enabled = true;

        /**
         * If X-Forwarded-For is enabled.
         */
        private boolean forEnabled = true;

        /**
         * If X-Forwarded-Host is enabled.
         */
        private boolean hostEnabled = true;

        /**
         * If X-Forwarded-Port is enabled.
         */
        private boolean portEnabled = true;

        /**
         * If X-Forwarded-Proto is enabled.
         */
        private boolean protoEnabled = true;

        /**
         * If X-Forwarded-Prefix is enabled.
         */
        private boolean prefixEnabled = true;

        /**
         * If appending X-Forwarded-For as a list is enabled.
         */
        private boolean forAppend = true;

        /**
         * If appending X-Forwarded-Host as a list is enabled.
         */
        private boolean hostAppend = true;

        /**
         * If appending X-Forwarded-Port as a list is enabled.
         */
        private boolean portAppend = true;

        /**
         * If appending X-Forwarded-Proto as a list is enabled.
         */
        private boolean protoAppend = true;

        /**
         * If appending X-Forwarded-Prefix as a list is enabled.
         */
        private boolean prefixAppend = true;

        public HttpHeaders filter(HttpHeaders input, HttpServletRequest request) {

            HttpHeaders updated = new HttpHeaders();

            for (Map.Entry<String, List<String>> entry : input.entrySet()) {
                updated.addAll(entry.getKey(), entry.getValue());
            }
            val remoteAddress = request.getRemoteAddr();

            if (isForEnabled() && remoteAddress != null) {
                write(updated, X_FORWARDED_FOR_HEADER, remoteAddress, isForAppend());
            }
            try {
                val uri = new URI(request.getRequestURI());
                String proto = uri.getScheme();
                if (isProtoEnabled()) {
                    write(updated, X_FORWARDED_PROTO_HEADER, proto, isProtoAppend());
                }

                if (isPortEnabled()) {
                    String port = String.valueOf(uri.getPort());
                    if (uri.getPort() < 0) {
                        port = String.valueOf(getDefaultPort(proto));
                    }
                    write(updated, X_FORWARDED_PORT_HEADER, port, isPortAppend());
                }

                if (isHostEnabled()) {
                    String host = toHostHeader(uri);
                    write(updated, X_FORWARDED_HOST_HEADER, host, isHostAppend());
                }
            }
            catch (URISyntaxException e) {
                // nothing to do
            }

            return updated;
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

        private String toHostHeader(URI uri) {
            int port = uri.getPort();
            String host = uri.getHost();
            String scheme = uri.getScheme();
            if (port < 0 || (port == HTTP_PORT && HTTP_SCHEME.equals(scheme))
                    || (port == HTTPS_PORT && HTTPS_SCHEME.equals(scheme))) {
                return host;
            }
            else {
                return host + ":" + port;
            }
        }

    }

}
