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
import com.apzda.cloud.gsvc.server.IServiceMethodHandler;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClientRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.apzda.cloud.gsvc.core.GtwRouterFunctionFactoryBean.ATTR_MATCHED_SEGMENTS;
import static com.apzda.cloud.gsvc.server.IServiceMethodHandler.GTW;

@Slf4j
@RequiredArgsConstructor
public class ProxyExchangeHandler implements ApplicationContextAware {

    private static final Logger webLog = LoggerFactory.getLogger(ProxyExchangeHandler.class);

    private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;

    private final GsvcExceptionHandler exceptionHandler;

    private final GatewayServiceConfigure serviceConfigure;

    private ApplicationContext applicationContext;

    private volatile List<HttpHeadersFilter> headersFilters;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ServerResponse handle(ServerRequest request, Route route, ServiceInfo serviceInfo) {
        val context = GsvcContextHolder.getContext();
        context.setAttributes(RequestContextHolder.getRequestAttributes());
        val serviceName = serviceInfo.getServiceName();
        context.setSvcName(serviceName);

        val httpRequest = request.servletRequest();
        val cfgName = serviceInfo.getCfgName();
        val client = this.applicationContext.getBean(ServiceMethod.getStubClientBeanName(cfgName), WebClient.class);
        val pattern = route.getMethod();
        var method = request.method();
        val params = request.uri().getQuery();
        val charset = Optional.ofNullable(httpRequest.getCharacterEncoding()).map(encoding -> {
            try {
                return Charset.forName(encoding);
            }
            catch (Exception e) {
                webLog.warn("Cannot use charset({}) for {}, use UTF-8 instead: {}", encoding, request.path(),
                        e.getMessage());
                return StandardCharsets.UTF_8;
            }
        }).orElse(StandardCharsets.UTF_8);
        val servletServerHttpRequest = new ServletServerHttpRequest(request.servletRequest());
        val filtered = HttpHeadersFilter.filterRequest(getHeadersFilters(), servletServerHttpRequest);

        String uri;

        List<? extends IPlugin> plugins;

        if (pattern.charAt(0) != '/') {
            uri = parseUri(request, pattern, !StringUtils.startsWith(pattern, "{"));
            val excludes = serviceConfigure.getExcludes(serviceName);
            if (excludes.contains(uri)) {
                return ServerResponse.status(HttpStatus.NOT_FOUND).build();
            }

            val serviceMethod = GatewayServiceRegistry.getServiceMethod(serviceInfo.getClazz(), uri);
            if (serviceMethod == null) {
                plugins = serviceConfigure.getGlobalPlugins();
            }
            else {
                plugins = serviceMethod.getPlugins();
            }
            uri = "/~" + serviceName + "/" + uri;
            method = HttpMethod.POST; // Internal calls between microservices
            filtered.add(IServiceMethodHandler.CALLER_HEADER, GTW);
        }
        else {
            uri = parseUri(request, pattern, false);
            val excludes = serviceConfigure.getExcludes(serviceName);
            if (excludes.contains(uri)) {
                return ServerResponse.status(HttpStatus.NOT_FOUND).build();
            }
            filtered.remove(IServiceMethodHandler.CALLER_HEADER);
            plugins = serviceConfigure.getGlobalPlugins();
        }

        val readTimeout = route.getReadTimeout();
        val requestUri = uri;

        webLog.debug("Proxy {} to {}:{} with: readTimeout({}), charset({}), param({}), headers({})", request.path(),
                cfgName, uri, readTimeout, charset, params, filtered);

        val body = BodyInserters.fromDataBuffers(DataBufferUtils.readInputStream(httpRequest::getInputStream,
                DefaultDataBufferFactory.sharedInstance, 1024));

        var proxyRequest = client.method(method).uri(uri + (StringUtils.isNotBlank(params) ? "?" + params : ""));

        if (readTimeout.toMillis() > 0) {
            proxyRequest = proxyRequest.httpRequest(httpReq -> {
                HttpClientRequest nr = httpReq.getNativeRequest();
                nr.responseTimeout(readTimeout);
            });
        }

        for (IPlugin plugin : plugins) {
            if (plugin instanceof IForwardPlugin prePlugin) {
                proxyRequest = prePlugin.preForward(serviceInfo, proxyRequest, uri);
            }
        }

        var proxyResponse = proxyRequest.headers(headers -> {
            headers.add("X-Request-ID", GsvcContextHolder.getRequestId());
            headers.putAll(filtered);
            headers.remove(HttpHeaders.HOST);
        }).body(body).exchangeToFlux(response -> {
            context.restore();

            val responseStatus = response.statusCode();
            ServerResponse.BodyBuilder serverResponse = ServerResponse.status(responseStatus);
            if (responseStatus == HttpStatus.UNAUTHORIZED) {
                val loginURL = ResponseUtils.getLoginUrl(request.headers().accept());
                if (loginURL != null) {
                    // renew a ServerResponse
                    serverResponse = ServerResponse.status(HttpStatus.TEMPORARY_REDIRECT)
                        .location(URI.create(loginURL))
                        .contentType(MediaType.TEXT_HTML);
                    log.error("Redirect to {}", loginURL);
                    return Flux.just(serverResponse.build());
                }
            }

            val headers = HttpHeadersFilter.filter(getHeadersFilters(), response.headers().asHttpHeaders(),
                    servletServerHttpRequest, HttpHeadersFilter.Type.RESPONSE);

            val httpHeaders = new HttpHeaders();
            httpHeaders.addAll(headers);

            if (httpHeaders.containsKey(HttpHeaders.TRANSFER_ENCODING)
                    && httpHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                // It is not valid to have both the transfer-encoding header and
                // the content-length header.
                // Remove the transfer-encoding header in the response if the
                // content-length header is present.
                httpHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
            }
            serverResponse.headers(header -> header.addAll(httpHeaders));
            // 缓存响应流
            @SuppressWarnings("all")
            val dataBuffers = response.body(BodyExtractors.toDataBuffers()).toStream();
            val resp = serverResponse.build((req, res) -> {
                context.restore();
                try (val writer = res.getOutputStream()) {
                    dataBuffers.forEach(dataBuffer -> {
                        webLog.trace("Read data from upstream({}:{}): {}", cfgName, requestUri, dataBuffer.capacity());
                        try (val input = dataBuffer.asInputStream()) {
                            writer.write(input.readAllBytes());
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                return null;
            });

            return Flux.just(resp);
        });

        var size = plugins.size();

        while (--size >= 0) {
            val plugin = plugins.get(size);
            if (plugin instanceof IForwardPlugin postPlugin) {
                proxyResponse = postPlugin.postForward(serviceInfo, proxyResponse, uri, method);
            }
        }

        proxyResponse = proxyResponse.onErrorResume(error -> {
            context.restore();
            val resp = exceptionHandler.handle(error.getCause() != null ? error.getCause() : error, httpRequest);
            return Flux.just(resp);
        }).onErrorComplete();

        // the proxyResponse must have only one ServerResponse
        request.servletRequest().setAttribute("GSVC.CONTEXT", context);
        return ServerResponse.async(proxyResponse.elementAt(0),
                Duration.ofMillis((long) (readTimeout.toMillis() * 1.2)));
    }

    public List<HttpHeadersFilter> getHeadersFilters() {
        if (headersFilters == null) {
            headersFilters = headersFiltersProvider.getIfAvailable();
        }
        return headersFilters;
    }

    @SuppressWarnings("unchecked")
    public static String parseUri(ServerRequest request, String pattern, boolean upper) {
        return request.attribute(ATTR_MATCHED_SEGMENTS).map((segments) -> {
            var template = pattern;
            for (val sg : ((Map<String, String>) segments).entrySet()) {
                val value = sg.getValue();
                template = template.replace("{" + sg.getKey() + "}", upper ? StringUtils.capitalize(value) : value);
            }
            return template;
        }).orElse(pattern);
    }

}
