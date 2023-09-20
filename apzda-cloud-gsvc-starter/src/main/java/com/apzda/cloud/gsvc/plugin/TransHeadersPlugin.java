/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpCookie;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Map;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class TransHeadersPlugin implements IPreCall, IGlobalPlugin {

    private final ApplicationContext context;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public WebClient.RequestBodySpec preCall(WebClient.RequestBodySpec request, Object data, ServiceMethod method) {
        val headers = GsvcContextHolder.headers("X-");

        val appName = context.getEnvironment().getProperty("spring.application.name");
        headers.remove("x-gsvc-caller");
        headers.put("x-gsvc-caller", appName);

        val requestId = GsvcContextHolder.getRequestId();
        headers.put("X-Request-Id", requestId);

        request = request.headers(httpHeaders -> {
            for (Map.Entry<String, String> kv : headers.entrySet()) {
                httpHeaders.put(kv.getKey(), Collections.singletonList(kv.getValue()));
            }
        });

        if (log.isTraceEnabled()) {
            log.trace("[{}] Transit Headers: {}", requestId, headers);
        }

        val cookies = GsvcContextHolder.cookies("xgh_");
        if (!cookies.isEmpty()) {
            request = request.cookies(cookie -> {
                for (Map.Entry<String, HttpCookie> kv : cookies.entrySet()) {
                    cookie.put(kv.getKey(), Collections.singletonList(kv.getValue().getValue()));
                }
            });

            if (log.isTraceEnabled()) {
                log.trace("[{}] Transit Cookies: {}", requestId, cookies);
            }
        }

        return request;
    }

}
