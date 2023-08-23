package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.map.MapUtil;
import com.apzda.cloud.gsvc.ResponseUtils;
import com.apzda.cloud.gsvc.ServiceError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.MethodDescriptor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpCookie;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 本地服务调用远程服务时使用的远程服务代理.
 * <p></p>
 * 本代理使用reactive loadbalancer去调用远程服务.
 * 在使用之前需要以下配置:
 *
 * <ul>
 *     <li></li>
 * </ul>
 *
 * @author ninggf
 */
@Slf4j
public class ReferenceServiceProxy implements InvocationHandler {
    private final String appName;
    private final String serviceName;
    private final ReactorLoadBalancerExchangeFilterFunction lbFunction;
    private final String url;
    private final ServiceConfigurationProperties.ServiceConfig serviceConfig;
    private final GatewayServiceConfigure svcConfigure;
    private final ObjectMapper objectMapper;
    private final int serviceIndex;
    private ReactiveCircuitBreaker circuitBreaker;

    public ReferenceServiceProxy(ReferenceServiceFactoryBean serviceProxyFactoryBean) {
        ApplicationContext applicationContext = serviceProxyFactoryBean.getApplicationContext();
        serviceName = serviceProxyFactoryBean.getId();
        appName = serviceProxyFactoryBean.getAppName();
        serviceIndex = serviceProxyFactoryBean.getIndex();
        objectMapper = ResponseUtils.OBJECT_MAPPER;

        lbFunction = applicationContext.getBean(ReactorLoadBalancerExchangeFilterFunction.class);
        svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);

        serviceConfig = svcConfigure.getServiceConfig(serviceProxyFactoryBean.getIndex());

        if (serviceConfig.isCircuitBreakerEnabled()) {
            val circuitBreakerFactory = applicationContext.getBean(ReactiveResilience4JCircuitBreakerFactory.class);
            circuitBreaker = circuitBreakerFactory.create(serviceName, appName);
        }

        log.info("For Service '{}@{}' Will try picking an instance via load-balancing: lb://{}",
            serviceName,
            appName,
            appName);

        url = "lb://" + appName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        val methodName = method.getName();
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(appName, serviceName, methodName);
        if (serviceMethod == null) {
            throw new NoSuchMethodException(String.format("%s@%s/%s", serviceName, appName, methodName));
        }
        return doHttpCall(methodName, args[0], serviceMethod);
    }

    private Object doHttpCall(String methodName, Object request, GatewayServiceRegistry.MethodInfo methodInfo) {
        String uri = "/" + appName + "/" + serviceName + "/" + methodName;
        Class<?> rClass = methodInfo.getReturnType();

        log.debug("rpc Service: {}{}", url, uri);

        var req = WebClient.builder()
            .baseUrl(url)
            .filter(lbFunction)
            .build()
            .post()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON);

        var reqBody = prepareRequestBody(req, request);

        MethodDescriptor.MethodType methodType = methodInfo.getType();
        return switch (methodType) {
            case UNARY, SERVER_STREAMING -> doBlockCall(reqBody, methodName, uri, rClass);
            default -> doAsyncCall(reqBody, methodName, uri, rClass);
        };
    }

    private Mono<Object> doAsyncCall(Mono<String> reqBody, String methodName, String uri, Class<?> rClass) {
        var reqMono = reqBody.handle((res, sink) -> {
            if (log.isDebugEnabled()) {
                log.debug("Response from svc://{}: {}", uri, StringUtils.truncate(res, 128));
            }
            sink.next(ResponseUtils.parseResponse(res, rClass));
            sink.complete();
        }).timeout(svcConfigure.getReadTimeout(serviceIndex, methodName));

        if (circuitBreaker != null) {
            //bookmark: 熔断处理
            reqMono = reqMono.transform(it -> circuitBreaker.run(it, Mono::error));
        }
        // bookmark: rpc fallback
        return reqMono
            .doOnError(err -> {
                if (log.isTraceEnabled()) {
                    log.error("(BIDI-STREAMING) rpc failed on svc://{}: ", uri, err);
                } else {
                    log.error("(BIDI-STREAMING) rpc failed on svc://{}: {}", uri, err.getMessage());
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

    private Object doBlockCall(Mono<String> reqBody, String methodName, String uri, Class<?> rClass) {
        reqBody = reqBody.timeout(svcConfigure.getReadTimeout(serviceIndex, methodName));

        if (circuitBreaker != null) {
            //BOOKMARK 熔断处理
            reqBody = reqBody.transform(it -> circuitBreaker.run(it, Mono::error));
        }
        // bookmark: fallback
        val res = reqBody
            .doOnError(err -> {
                if (log.isTraceEnabled()) {
                    log.error("(UNARY,SERVER_STREAMING) rpc failed on svc://{}: ", uri, err);
                } else {
                    log.error("(UNARY,SERVER_STREAMING) rpc failed on svc://{}: {}", uri, err.getMessage());
                }
            })
            .onErrorReturn(WebClientResponseException.Unauthorized.class,
                ServiceError.REMOTE_SERVICE_UNAUTHORIZED.fallbackString(serviceName))
            .onErrorReturn(WebClientResponseException.Forbidden.class,
                ServiceError.REMOTE_SERVICE_FORBIDDEN.fallbackString(serviceName))
            .onErrorReturn(WebClientResponseException.NotFound.class,
                ServiceError.REMOTE_SERVICE_NOT_FOUND.fallbackString(serviceName))
            .onErrorReturn(TimeoutException.class, ServiceError.REMOTE_SERVICE_TIMEOUT.fallbackString(serviceName))
            .onErrorReturn(WebClientRequestException.class,
                ServiceError.REMOTE_SERVICE_NO_INSTANCE.fallbackString(serviceName))
            .onErrorReturn(ServiceError.REMOTE_SERVICE_ERROR.fallbackString(serviceName))
            .block();

        if (log.isDebugEnabled()) {
            log.debug("Response from svc://{}: {}", uri, StringUtils.truncate(res, 128));
        }

        return ResponseUtils.parseResponse(res, rClass);
    }

    @SuppressWarnings("unchecked")
    private Mono<String> prepareRequestBody(WebClient.RequestBodySpec req, Object requestObj) {

        val headers = GsvcContextHolder.headers("x-gh-");
        if (StpUtil.isLogin()) {
            //bookmark 透传sa-token登录信息
            val tokenInfo = StpUtil.getTokenInfo();
            log.trace("Set Sa-Token header: '{}: {}'", tokenInfo.tokenName, tokenInfo.tokenValue);
            headers.put(tokenInfo.tokenName, tokenInfo.tokenValue);
        }
        // X-Forwarded-For
        val forwards = GsvcContextHolder.headers("X-Forwarded-");
        headers.putAll(forwards);
        //bookmark 透传请求头
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

        val request = req.contentType(MediaType.APPLICATION_JSON).acceptCharset(StandardCharsets.UTF_8)
            .body(BodyInserters.fromPublisher(((Mono<Object>) requestObj).handle(
                    (obj, sink) -> {
                        try {
                            sink.next(objectMapper.writeValueAsString(obj));
                            sink.complete();
                        } catch (JsonProcessingException e) {
                            log.error("请求请求体出错: {}", e.getMessage());
                            sink.error(e);
                        }
                    }
                ), String.class)
            );

        return request.retrieve().bodyToMono(String.class);
    }

    @Override
    public String toString() {
        return new ToStringCreator(this)
            .append("appName", appName)
            .append("service", serviceName)
            .append("index", serviceIndex)
            .append("cb", circuitBreaker != null)
            .toString();
    }
}
