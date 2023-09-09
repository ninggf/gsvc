package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultServiceCaller implements IServiceCaller {

    private final WebClient webClient;

    private final ApplicationContext applicationContext;

    private final ObjectMapper objectMapper = ResponseUtils.OBJECT_MAPPER;

    private final GatewayServiceConfigure svcConfigure;

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public <T, R> R unaryCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz) {
        val serviceName = GatewayServiceRegistry.svcName(clazz);
        val url = getServiceUrl(clazz, method);
        var req = webClient.post().uri(url).accept(MediaType.APPLICATION_JSON);
        var reqBody = prepareRequestBody(req, request);
        return doBlockCall(reqBody, serviceName, method, url, resClazz);
    }

    @Override
    public <T, R> Mono<R> serverStreamingCall(Class<?> clazz, String method, T request, Class<T> reqClazz,
            Class<R> resClazz) {
        val serviceName = GatewayServiceRegistry.svcName(clazz);
        val url = getServiceUrl(clazz, method);
        var req = webClient.post().uri(url).accept(MediaType.APPLICATION_JSON);
        var reqBody = prepareRequestBody(req, request);
        return doAsyncCall(reqBody, serviceName, method, url, resClazz);
    }

    @Override
    public <T, R> Mono<R> bidiStreamingCall(Class<?> clazz, String method, Mono<T> request, Class<T> reqClazz,
            Class<R> resClazz) {
        return serverStreamingCall(clazz, method, request.block(), reqClazz, resClazz);
    }

    private <R> R doBlockCall(Mono<String> reqBody, String serviceName, String methodName, String uri,
            Class<R> rClass) {
        // bookmark: block rpc
        reqBody = reqBody.timeout(svcConfigure.getReadTimeout(serviceName, methodName));

        val res = handleRpcFallback(reqBody, serviceName, uri, String.class).block();

        if (log.isDebugEnabled()) {
            log.debug("[{}] Response from svc://{}: {}", GsvcContextHolder.getRequestId(), uri,
                    StringUtils.truncate(res, 256));
        }

        return ResponseUtils.parseResponse(res, rClass);
    }

    private <R> Mono<R> doAsyncCall(Mono<String> reqBody, String serviceName, String methodName, String uri,
            Class<R> rClass) {
        // bookmark: async rpc
        var reqMono = reqBody.<R>handle((res, sink) -> {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Response from svc://{}: {}", GsvcContextHolder.getRequestId(), uri,
                        StringUtils.truncate(res, 256));
            }
            sink.next(ResponseUtils.parseResponse(res, rClass));
            sink.complete();
        }).timeout(svcConfigure.getReadTimeout(serviceName, methodName));

        return handleRpcFallback(reqMono, serviceName, uri, rClass);
    }

    private <R> Mono<R> handleRpcFallback(Mono<R> reqBody, String serviceName, String uri, Class<R> rClass) {
        // bookmark: fallback
        val requestId = GsvcContextHolder.getRequestId();
        return reqBody.doOnError(err -> {
            if (log.isTraceEnabled()) {
                log.error("[{}] RPC failed on svc://{}: ", requestId, uri, err);
            }
            else {
                log.error("[{}] RPC failed on svc://{}: {}", requestId, uri, err.getMessage());
            }
        })
            .onErrorReturn(WebClientResponseException.Unauthorized.class,
                    ResponseUtils.fallback(ServiceError.REMOTE_SERVICE_UNAUTHORIZED, serviceName, rClass))
            .onErrorReturn(WebClientResponseException.Forbidden.class,
                    ResponseUtils.fallback(ServiceError.REMOTE_SERVICE_FORBIDDEN, serviceName, rClass))
            .onErrorReturn(WebClientResponseException.NotFound.class,
                    ResponseUtils.fallback(ServiceError.REMOTE_SERVICE_NOT_FOUND, serviceName, rClass))
            .onErrorReturn(TimeoutException.class,
                    ResponseUtils.fallback(ServiceError.REMOTE_SERVICE_TIMEOUT, serviceName, rClass))
            .onErrorReturn(WebClientRequestException.class,
                    ResponseUtils.fallback(ServiceError.REMOTE_SERVICE_NO_INSTANCE, serviceName, rClass))
            .onErrorReturn(ResponseUtils.fallback(ServiceError.REMOTE_SERVICE_ERROR, serviceName, rClass));
    }

    protected String getServiceUrl(Class<?> clazz, String method) {
        val svcName = GatewayServiceRegistry.svcName(clazz);
        return String.format("http://%s/%s/%s", svcName, svcName, method);
    }

    @SuppressWarnings("unchecked")
    private Mono<String> prepareRequestBody(WebClient.RequestBodySpec req, Object requestObj) {
        val headers = GsvcContextHolder.headers("x-gh-");
        headers.put("X-Gsvc-Caller", appName);
        val requestId = GsvcContextHolder.getRequestId();
        headers.put("X-Request-Id", requestId);
        // X-Forwarded-For
        val forwards = GsvcContextHolder.headers("X-Forwarded-");
        headers.putAll(forwards);

        // bookmark 透传请求头
        if (!headers.isEmpty()) {
            req = req.headers(httpHeaders -> {
                for (Map.Entry<String, String> kv : headers.entrySet()) {
                    httpHeaders.put(kv.getKey(), Collections.singletonList(kv.getValue()));
                }
            });
        }
        // 透传 xgh_开头的COOKIE
        val cookies = GsvcContextHolder.cookies("xgh_");
        if (!cookies.isEmpty()) {
            req = req.cookies(cookie -> {
                for (Map.Entry<String, HttpCookie> kv : cookies.entrySet()) {
                    cookie.put(kv.getKey(), Collections.singletonList(kv.getValue().getValue()));
                }
            });
        }

        if (!(requestObj instanceof Mono)) {
            requestObj = Mono.just(requestObj);
        }

        val request = req.contentType(MediaType.APPLICATION_JSON)
            .acceptCharset(StandardCharsets.UTF_8)
            .body(BodyInserters.fromPublisher(((Mono<Object>) requestObj).handle((obj, sink) -> {
                try {
                    sink.next(objectMapper.writeValueAsString(obj));
                    sink.complete();
                }
                catch (JsonProcessingException e) {
                    log.error("[{}] Bad response: {}", GsvcContextHolder.getRequestId(), e.getMessage());
                    sink.error(e);
                }
            }), String.class));

        return request.retrieve().bodyToMono(String.class);
    }

}
