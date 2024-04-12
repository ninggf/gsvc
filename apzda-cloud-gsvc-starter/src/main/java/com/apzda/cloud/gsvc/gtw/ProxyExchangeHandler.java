package com.apzda.cloud.gsvc.gtw;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceInfo;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.gtw.filter.HttpHeadersFilter;
import com.apzda.cloud.gsvc.plugin.IForwardPlugin;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.apzda.cloud.gsvc.core.GtwRouterFunctionFactoryBean.ATTR_MATCHED_SEGMENTS;

@Slf4j
@RequiredArgsConstructor
public class ProxyExchangeHandler implements ApplicationContextAware {

    private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;

    private final GsvcExceptionHandler exceptionHandler;

    private final GatewayServiceConfigure serviceConfigure;

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
        val method = request.method();
        val params = request.uri().getQuery();
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
        val servletServerHttpRequest = new ServletServerHttpRequest(request.servletRequest());
        val filtered = HttpHeadersFilter.filterRequest(getHeadersFilters(), servletServerHttpRequest);

        String uri = request.attribute(ATTR_MATCHED_SEGMENTS).map((segments) -> {
            var template = pattern;
            for (val sg : ((Map<String, String>) segments).entrySet()) {
                template = template.replace("{" + sg.getKey() + "}", sg.getValue());
            }
            return template;
        }).orElse(pattern);
        List<? extends IPlugin> plugins;

        if (pattern.charAt(0) != '/') {
            uri = "/~" + context.getSvcName() + "/" + uri;
            filtered.add("X-Gsvc-Caller", "gtw");
            val serviceMethod = GatewayServiceRegistry.getServiceMethod(serviceInfo.getClazz(), uri);
            plugins = serviceMethod.getPlugins();
        }
        else {
            plugins = serviceConfigure.getGlobalPlugins();
        }

        log.trace("Proxy {} to {}{} with: charset({}), param({}), headers({})", request.path(), cfgName, uri, charset,
                params, filtered);

        val body = BodyInserters.fromDataBuffers(DataBufferUtils.readInputStream(httpRequest::getInputStream,
                DefaultDataBufferFactory.sharedInstance, 1024));

        var proxyRequest = client.method(method).uri(uri + (StringUtils.isNotBlank(params) ? "?" + params : ""));

        for (IPlugin plugin : plugins) {
            if (plugin instanceof IForwardPlugin prePlugin) {
                proxyRequest = prePlugin.preForward(proxyRequest, uri);
            }
        }

        var proxyResponse = proxyRequest.headers(headers -> {
            headers.add("X-Request-ID", GsvcContextHolder.getRequestId());
            headers.putAll(filtered);
            headers.remove(HttpHeaders.HOST);
        }).body(body).exchangeToFlux(response -> {
            context.restore();
            val headers = HttpHeadersFilter.filter(getHeadersFilters(), response.headers().asHttpHeaders(),
                    servletServerHttpRequest, HttpHeadersFilter.Type.RESPONSE);

            val httpHeaders = new HttpHeaders();
            httpHeaders.addAll(headers);
            httpHeaders.remove("X-Request-ID");

            if (!httpHeaders.containsKey(HttpHeaders.TRANSFER_ENCODING)
                    && httpHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                // It is not valid to have both the transfer-encoding header and
                // the content-length header.
                // Remove the transfer-encoding header in the response if the
                // content-length header is present.
                httpHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
            }

            // log.trace("Response Headers: {}", httpHeaders);

            val serverResponse = ServerResponse.status(response.statusCode())
                .headers(httpHeaders1 -> httpHeaders1.addAll(httpHeaders));
            val stopWatch = new StopWatch("缓存响应流");
            stopWatch.start();
            // 缓存响应流
            val dataBuffers = response.body(BodyExtractors.toDataBuffers()).toStream();
            stopWatch.stop();
            log.trace("{}", stopWatch.prettyPrint(TimeUnit.MILLISECONDS));

            val resp = serverResponse.build((req, res) -> {
                try (val writer = res.getOutputStream()) {
                    dataBuffers.forEach(dataBuffer -> {
                        log.trace("Read data from downstream: {}", dataBuffer.capacity());
                        try (val input = dataBuffer.asInputStream()) {
                            writer.write(input.readAllBytes());
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                log.trace("Proxy Response Body sent!");
                return null;
            });

            return Flux.just(resp);
        });

        var size = plugins.size();
        while (--size >= 0) {
            val plugin = plugins.get(size);
            if (plugin instanceof IForwardPlugin postPlugin) {
                proxyResponse = postPlugin.postForward(proxyResponse, uri);
            }
        }

        proxyResponse = proxyResponse.onErrorResume(error -> {
            context.restore();
            log.warn("Proxy for {} error: {}", request.path(), error.getMessage());
            val resp = exceptionHandler.handle(error.getCause() != null ? error.getCause() : error, httpRequest);
            return Flux.just(resp);
        }).onErrorComplete();

        return ServerResponse.async(proxyResponse.elementAt(0));
    }

    public List<HttpHeadersFilter> getHeadersFilters() {
        if (headersFilters == null) {
            headersFilters = headersFiltersProvider.getIfAvailable();
        }
        return headersFilters;
    }

}
