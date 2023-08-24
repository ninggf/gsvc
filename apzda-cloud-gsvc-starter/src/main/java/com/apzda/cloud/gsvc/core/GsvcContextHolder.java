package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.http.HttpCookie;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author ninggf
 */
class GsvcContextHolder {
    public static Optional<SaTokenContext> getExchange() {
        return Optional.ofNullable(SaHolder.getContext());
    }

    public static Optional<HttpServletRequest> getRequest() {
        HttpServletRequest request =
            ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
        return Optional.of(request);
    }

    public static Map<String, String> headers() {
        val headers = new HashMap<String, String>();

        return headers;
    }

    public static Map<String, String> headers(String prefix) {
        val headers = new HashMap<String, String>();

        return headers;
    }

    public static Map<String, HttpCookie> cookies() {
        val cookies = new HashMap<String, HttpCookie>();

        return cookies;
    }

    public static Map<String, HttpCookie> cookies(String prefix) {
        val cookies = new HashMap<String, HttpCookie>();

        return cookies;
    }
}
