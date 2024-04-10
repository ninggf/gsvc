package com.apzda.cloud.gsvc.gtw;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceInfo;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.apzda.cloud.gsvc.core.GtwRouterFunctionFactoryBean.ATTR_MATCHED_SEGMENTS;

@Slf4j
@RequiredArgsConstructor
public class ProxyExchangeHandler implements ApplicationContextAware {

    private final static DefaultDataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;

    private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;

    private final GsvcExceptionHandler exceptionHandler;

    private ApplicationContext applicationContext;

    private volatile List<HttpHeadersFilter> headersFilters;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings("unchecked")
    public ServerResponse handle(ServerRequest request, Route route, ServiceInfo serviceInfo) {
        val context = GsvcContextHolder.current();
        context.setAttributes(RequestContextHolder.getRequestAttributes());
        context.setSvcName(serviceInfo.getServiceName());

        val httpRequest = request.servletRequest();

        val cfgName = serviceInfo.getCfgName();
        val client = this.applicationContext.getBean(ServiceMethod.getStubClientBeanName(cfgName), WebClient.class);
        val pattern = route.getMethod();
        val uri = request.attribute(ATTR_MATCHED_SEGMENTS).map((segments) -> {
            var template = pattern;
            for (val sg : ((Map<String, String>) segments).entrySet()) {
                template = template.replace("{" + sg.getKey() + "}", sg.getValue());
            }
            return template;
        }).orElse(pattern);

        val method = request.method();
        val params = httpRequest.getParameterMap();
        val charset = Optional.ofNullable(httpRequest.getCharacterEncoding()).map(encoding -> {
            try {
                return Charset.forName(encoding);
            }
            catch (Exception e) {
                log.warn("Cannot use charset({}) for {}, use UTF-8 instead: {}", encoding, request.path(),
                        e.getMessage());
                return StandardCharsets.UTF_8;
            }
        }).orElse(StandardCharsets.UTF_8);

        val filtered = HttpHeadersFilter.filterRequest(getHeadersFilters(), request);

        log.trace("Proxy {} to {}{} with: charset({}), param({}), headers({})", request.path(), cfgName, uri, charset,
                params, filtered);

        val content = new ContentCachingRequestWrapper(httpRequest);

        BufferedReader reader;
        try {
            reader = content.getReader();
        }
        catch (IOException e) {
            return ServerResponse.status(500).body(Response.error(ServiceError.SERVICE_ERROR));
        }

        val proxyResponse = client.method(method).uri(uri, params).headers(headers -> {
            headers.putAll(filtered);
            headers.remove(HttpHeaders.HOST);
        })
            .body(BodyInserters.fromDataBuffers(
                    Flux.fromStream(reader::lines).map(str -> dataBufferFactory.wrap(str.getBytes(charset)))))
            .exchangeToMono((response) -> response.toBodilessEntity().mapNotNull((resp) -> {
                context.restore();
                val headers = HttpHeadersFilter.filter(getHeadersFilters(), resp.getHeaders(), request,
                        HttpHeadersFilter.Type.RESPONSE);
                val httpHeaders = new HttpHeaders();
                httpHeaders.addAll(headers);
                if (!httpHeaders.containsKey(HttpHeaders.TRANSFER_ENCODING)
                        && httpHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                    // It is not valid to have both the transfer-encoding header and
                    // the content-length header.
                    // Remove the transfer-encoding header in the response if the
                    // content-length header is present.
                    httpHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                }
                log.trace("Got Proxy response: {}", resp);
                val serverResponse = ServerResponse.status(resp.getStatusCode())
                    .headers(httpHeaders1 -> httpHeaders1.addAll(httpHeaders));
                return serverResponse.build((req, res) -> {
                    res.getWriter().write("Hello");
                    res.getWriter().close();
                    return null;
                });
            }))
            .onErrorResume((error) -> {
                context.restore();
                if (log.isTraceEnabled()) {
                    log.trace("proxy for {} error: {}", request.path(), error.getMessage());
                }
                if (error instanceof Exception) {
                    val responseEntity = exceptionHandler.handleException((Exception) error, httpRequest);
                    return Mono.just(ServerResponse.status(responseEntity.getStatusCode()).body(responseEntity));
                }
                return Mono.just(ServerResponse.status(502).body(HttpStatus.BAD_GATEWAY.getReasonPhrase()));
            });
        // 此处要转换请求头a
        return ServerResponse.async(proxyResponse);
    }

    public List<HttpHeadersFilter> getHeadersFilters() {
        if (headersFilters == null) {
            headersFilters = headersFiltersProvider.getIfAvailable();
        }
        return headersFilters;
    }

}
