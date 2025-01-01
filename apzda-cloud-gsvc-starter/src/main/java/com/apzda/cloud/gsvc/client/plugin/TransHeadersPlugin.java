/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.client.plugin;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.gtw.filter.HttpHeadersFilter;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.server.IServiceMethodHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class TransHeadersPlugin implements IGlobalPlugin, IPreCall {

    private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;

    @Value("${spring.application.name:gsvc}")
    private String appName;

    private volatile List<HttpHeadersFilter> headersFilters;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @NonNull
    public WebClient.RequestBodySpec preCall(@NonNull WebClient.RequestBodySpec request, @Nullable Object data,
            @Nullable ServiceMethod method) {
        val headers = GsvcContextHolder.getRequest().map(curRequest -> {
            headersFilters = headersFiltersProvider.getIfAvailable();
            return HttpHeadersFilter.filterRequest(headersFilters, new ServletServerHttpRequest(curRequest));
        }).orElse(new HttpHeaders());

        headers.remove("Accept");
        headers.remove("Content-Type");
        headers.remove("Content-Length");
        headers.remove(IServiceMethodHandler.CALLER_HEADER);
        headers.remove(HttpHeaders.HOST);
        headers.remove(HttpHeaders.AUTHORIZATION);
        headers.add(IServiceMethodHandler.CALLER_HEADER, appName);

        val requestId = GsvcContextHolder.getRequestId();
        headers.add("X-Request-ID", requestId);

        request = request.headers(httpHeaders -> {
            httpHeaders.putAll(headers);
        });

        if (log.isTraceEnabled()) {
            log.trace("Transit Headers: {}", headers);
        }

        val cookies = GsvcContextHolder.cookies("xgh_");
        val lang = GsvcContextHolder.cookies().get("lang");
        if (lang != null) {
            cookies.put("lang", lang);
        }
        if (!cookies.isEmpty()) {
            request = request.cookies(cookie -> {
                for (Map.Entry<String, HttpCookie> kv : cookies.entrySet()) {
                    cookie.put(kv.getKey(), Collections.singletonList(kv.getValue().getValue()));
                }
            });

            if (log.isTraceEnabled()) {
                log.trace("Transit Cookies: {}", cookies);
            }
        }

        return request;
    }

}
