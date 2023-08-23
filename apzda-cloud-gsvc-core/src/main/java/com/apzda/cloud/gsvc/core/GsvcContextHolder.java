package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.val;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author ninggf
 */
public class GsvcContextHolder {
    public static Optional<ServerWebExchange> getExchange() {
        return Optional.ofNullable(SaReactorSyncHolder.getContext());
    }

    public static Optional<ServerHttpRequest> getRequest() {
        return getExchange().map(ServerWebExchange::getRequest);
    }

    public static Optional<ServerHttpResponse> getResponse() {
        return getExchange().map(ServerWebExchange::getResponse);
    }

    public static Map<String, String> headers() {
        val headers = new HashMap<String, String>();
        getExchange().ifPresent((r) -> {
            r.getAttributeOrDefault("filtered_http_headers", new DefaultHttpHeaders()).forEach((header) -> {
                headers.put(header.getKey(), header.getValue());
            });
        });
        return headers;
    }

    public static Map<String, String> headers(String prefix) {
        val headers = new HashMap<String, String>();
        getExchange().ifPresent((r) -> {
            r.getAttributeOrDefault("filtered_http_headers", new DefaultHttpHeaders()).forEach((header) -> {
                if (StringUtils.startsWithIgnoreCase(header.getKey(), prefix)) {
                    headers.put(header.getKey(), header.getValue());
                }
            });
        });
        return headers;
    }

    public static Map<String, HttpCookie> cookies() {
        val cookies = new HashMap<String, HttpCookie>();
        getRequest().ifPresent((r) -> {
            r.getCookies().forEach((k, vs) -> {
                cookies.put(k, vs.get(0));
            });
        });
        return cookies;
    }

    public static Map<String, HttpCookie> cookies(String prefix) {
        val cookies = new HashMap<String, HttpCookie>();
        getRequest().ifPresent((r) -> {
            r.getCookies().forEach((k, vs) -> {
                if (StringUtils.startsWithIgnoreCase(k, prefix)) {
                    cookies.put(k, vs.get(0));
                }
            });
        });
        return cookies;
    }
}
