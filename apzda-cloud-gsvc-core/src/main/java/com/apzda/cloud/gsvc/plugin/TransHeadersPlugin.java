/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpCookie;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

/**
 * @author fengz
 */
@Slf4j
public class TransHeadersPlugin implements IPrePlugin {

    public static final TransHeadersPlugin TRANS_HEADERS_PLUGIN = new TransHeadersPlugin();

    @Override
    public WebClient.RequestBodySpec preCall(WebClient.RequestBodySpec request, Mono<Object> data, ServiceMethod method,
            ApplicationContext context) {
        val appName = context.getEnvironment().getProperty("spring.application.name");
        val headers = GsvcContextHolder.headers("x-gh-");
        headers.put("X-Gsvc-Caller", appName);
        val requestId = GsvcContextHolder.getRequestId();
        headers.put("X-Request-Id", requestId);
        // X-Forwarded-For
        val forwards = GsvcContextHolder.headers("X-Forwarded-");
        headers.putAll(forwards);
        // bookmark: 透传请求头
        request = request.headers(httpHeaders -> {
            for (Map.Entry<String, String> kv : headers.entrySet()) {
                httpHeaders.put(kv.getKey(), Collections.singletonList(kv.getValue()));
            }
        });

        if (log.isTraceEnabled()) {
            log.trace("[{}] Transit Headers: {}", requestId, headers);
        }
        // bookmark: 透传 xgh_开头的COOKIE
        val cookies = GsvcContextHolder.cookies("xgh_");
        if (!cookies.isEmpty()) {
            request = request.cookies(cookie -> {
                for (Map.Entry<String, HttpCookie> kv : cookies.entrySet()) {
                    cookie.put(kv.getKey(), Collections.singletonList(kv.getValue().getValue()));
                }
            });
        }

        return request;
    }

}
