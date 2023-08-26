package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.SaManager;
import com.apzda.cloud.gsvc.filter.XForwardedHeadersFilter;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author ninggf
 */
public class GsvcContextHolder {

    private static final XForwardedHeadersFilter xForwardedHeadersFilter = new XForwardedHeadersFilter();

    private static final String FILTERED_HTTP_HEADERS = "FILTERED_HTTP_HEADERS";

    public static Optional<HttpServletRequest> getRequest() {
        if (SaManager.getSaTokenContext() != null && SaManager.getSaTokenContext().getRequest() != null) {
            val requestAttributes = SaManager.getSaTokenContext().getRequest().getSource();
            if (requestAttributes instanceof HttpServletRequest request) {
                return Optional.of(request);
            }
        }
        return Optional.empty();
    }

    public static Map<String, String> headers() {
        val headers = new HashMap<String, String>();
        if (getRequest().isPresent()) {
            HttpServletRequest httpServletRequest = getRequest().get();
            Object filtered = httpServletRequest.getAttribute(FILTERED_HTTP_HEADERS);
            if (filtered == null) {
                synchronized (FILTERED_HTTP_HEADERS) {
                    filtered = httpServletRequest.getAttribute(FILTERED_HTTP_HEADERS);
                    if (filtered == null) {
                        val httpHeaders = HttpHeaders.readOnlyHttpHeaders(createDefaultHttpHeaders(httpServletRequest));
                        HttpHeaders filtered1 = xForwardedHeadersFilter.filter(httpHeaders, httpServletRequest);
                        val defaultHttpHeaders = new DefaultHttpHeaders();
                        filtered1.forEach(defaultHttpHeaders::set);
                        httpServletRequest.setAttribute(FILTERED_HTTP_HEADERS, defaultHttpHeaders);
                        filtered = defaultHttpHeaders;
                    }
                }
            }
            if (filtered instanceof DefaultHttpHeaders defaultHttpHeaders) {
                defaultHttpHeaders.forEach((kv) -> {
                    headers.put(kv.getKey(), kv.getValue());
                });
            }
        }
        return headers;
    }

    public static String header(String name) {
        return headers().get(name.toLowerCase());
    }

    public static String headerIgnoreCase(String name) {
        val s = headers().keySet().stream().filter(name::equalsIgnoreCase).findFirst().orElse("-_-");
        return headers().get(s);
    }

    public static Map<String, String> headers(String prefix) {
        val headers = new HashMap<String, String>();
        headers().forEach((k, v) -> {
            if (StringUtils.startsWithIgnoreCase(k, prefix)) {
                headers.put(k, v);
            }
        });
        return headers;
    }

    public static Map<String, HttpCookie> cookies() {
        val cookies = new HashMap<String, HttpCookie>();
        if (getRequest().isPresent()) {
            val httpServletRequest = getRequest().get();
            val cookies1 = httpServletRequest.getCookies();
            if (cookies1 != null) {
                for (Cookie cookie : cookies1) {
                    cookies.put(cookie.getName(), new HttpCookie(cookie.getName(), cookie.getValue()));
                }
            }
        }
        return cookies;
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
        MultiValueMap<String, String> headers = CollectionUtils
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
        }
        return headers;
    }

    public static String getRequestId() {
        val requestId = headerIgnoreCase("x-request-id");
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        return "";
    }

}
