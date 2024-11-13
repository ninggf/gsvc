package com.apzda.cloud.gsvc.core;

import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import com.apzda.cloud.gsvc.autoconfigure.ConfigureHelper;
import com.google.common.base.Splitter;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

/**
 * @author ninggf
 */
@Slf4j
public abstract class GsvcContextHolder implements ApplicationContextAware {

    private static final String FILTERED_HTTP_HEADERS = "FILTERED_HTTP_HEADERS";

    private static final String HTTP_COOKIES = "FILTERED_HTTP_COOKIES";

    private static final ThreadLocal<GsvcContext> CONTEXT_BOX = new ThreadLocal<>();

    private static String appName;

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        GsvcContextHolder.appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        GsvcContextHolder.applicationContext = applicationContext;
    }

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
        val context = getContext();
        val headers = context.getHeaders();
        if (headers != null) {
            return headers;
        }

        val request = getRequest();
        if (request.isPresent()) {
            HttpServletRequest httpServletRequest = request.get();
            Object filtered = httpServletRequest.getAttribute(FILTERED_HTTP_HEADERS);
            if (filtered == null) {
                synchronized (httpServletRequest) {
                    filtered = httpServletRequest.getAttribute(FILTERED_HTTP_HEADERS);
                    if (filtered == null) {
                        val httpHeaders = HttpHeaders.readOnlyHttpHeaders(createDefaultHttpHeaders(httpServletRequest));
                        val defaultHttpHeaders = new DefaultHttpHeaders();
                        httpHeaders.forEach(defaultHttpHeaders::set);
                        httpServletRequest.setAttribute(FILTERED_HTTP_HEADERS, defaultHttpHeaders);
                        filtered = defaultHttpHeaders;
                        context.setHeaders(defaultHttpHeaders);
                        context.setRemoteAddr(httpServletRequest.getRemoteAddr());
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

    @NonNull
    public static Map<String, String> headers(String prefix) {
        val headers = new HashMap<String, String>();
        headers().forEach(header -> {
            if (StringUtils.startsWithIgnoreCase(header.getKey(), prefix)) {
                headers.put(header.getKey(), header.getValue());
            }
        });
        return headers;
    }

    @Nullable
    public static String cookie(String name) {
        val httpCookie = cookies(name).get(name);
        if (httpCookie != null) {
            return httpCookie.getValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static Map<String, HttpCookie> cookies() {
        val context = getContext();
        val cachedCookie = context.getCookies();
        if (cachedCookie != null) {
            return cachedCookie;
        }
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
                context.setCookies(cookies);
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
            val rid = request.getAttribute("X-Request-ID");
            if (rid != null) {
                headers.add("X-Request-ID", (String) rid);
            }
            else {
                headers.add("X-Request-ID", UUID.randomUUID().toString());
                request.setAttribute("X-Request-ID", headers.getFirst("X-Request-ID"));
            }
        }
        return headers;
    }

    @NonNull
    public static GsvcContext getContext() {
        var context = CONTEXT_BOX.get();
        if (context == null) {
            context = new GsvcContext("", null, getAppName());
            CONTEXT_BOX.set(context);
        }
        return context;
    }

    public static String getAppName() {
        return org.apache.commons.lang3.StringUtils.defaultIfBlank(appName,
                System.getProperty("spring.application.name", System.getenv("SPRING_APPLICATION_NAME")));
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Nonnull
    public static <T> T getBean(Class<T> beanClass) {
        if (applicationContext != null) {
            return applicationContext.getBean(beanClass);
        }

        throw new NullPointerException("applicationContext is null");
    }

    @Deprecated
    @NonNull
    public static GsvcContext current() {
        return getContext();
    }

    public static void restore(@NonNull GsvcContext context) {
        CONTEXT_BOX.set(context);
        MDC.put("tid", context.requestId);
    }

    public static void clear() {
        CONTEXT_BOX.remove();
    }

    public static String getRequestId() {
        val requestId = header("x-request-id");
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        val context = CONTEXT_BOX.get();
        if (context != null) {
            return org.apache.commons.lang3.StringUtils.defaultIfBlank(context.requestId, "");
        }
        else {
            return "";
        }
    }

    public static String getRemoteIp() {
        val context = getContext();
        if (StringUtils.hasText(context.remoteIp)) {
            return context.remoteIp;
        }

        val request = getRequest();
        var ip = org.apache.commons.lang3.StringUtils.defaultIfBlank(context.remoteAddr, "0.0.0.0"); // 未获取到IP

        var headers = context.headers;
        if (headers == null && request.isPresent()) {
            headers = headers();
        }

        if (headers == null) {
            log.warn("Headers are null, use default ip: {}", ip);
            context.remoteIp = ip;
            return ip;
        }

        val remoteAddr = context.remoteAddr;
        val froms = ConfigureHelper.getRealIpFrom();

        if (!CollectionUtils.isEmpty(froms)) {
            val remoteIp = headers.get(ConfigureHelper.getRealIpHeader());
            if (StringUtils.hasText(remoteIp)) {
                if (remoteAddr.contains(":") && froms.stream().anyMatch(from -> from.equals(remoteAddr))) {
                    context.remoteIp = remoteIp;
                    return remoteIp;
                }
                else if (froms.stream()
                    .anyMatch(from -> (from.contains("/") && NetUtil.isInRange(remoteAddr, from))
                            || Ipv4Util.matches(from, remoteAddr))) {
                    context.remoteIp = remoteIp;
                    return remoteIp;
                }
            }
        }

        val xForwardedFor = headers.get("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            val ips = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(xForwardedFor);
            if (!ips.isEmpty()) {
                context.remoteIp = ips.get(0);
                return context.remoteIp;
            }
        }

        context.remoteIp = remoteAddr;
        return context.remoteIp;
    }

    @Data
    public final static class GsvcContext {

        private String requestId;

        private RequestAttributes attributes;

        private String svcName;

        private DefaultHttpHeaders headers;

        private Map<String, HttpCookie> cookies;

        private Locale locale;

        private String caller;

        private String remoteIp;

        private String remoteAddr;

        private Attributes grpcAttributes;

        private Metadata grpcMetadata;

        GsvcContext(String requestId, RequestAttributes attributes, String svcName) {
            this.attributes = attributes;
            this.svcName = svcName;
            this.setRequestId(requestId);
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
            MDC.put("tid", requestId);
        }

        public void restore() {
            GsvcContextHolder.restore(this);
        }

        public String getRemoteIp() {
            return GsvcContextHolder.getRemoteIp();
        }

        @NonNull
        public static GsvcContext current() {

            return GsvcContextHolder.getContext();
        }

    }

}
