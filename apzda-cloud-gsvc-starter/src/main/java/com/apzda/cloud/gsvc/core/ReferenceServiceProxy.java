package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.stp.StpUtil;
import com.apzda.cloud.gsvc.ResponseUtils;
import com.apzda.cloud.gsvc.ServiceError;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
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
 * 本代理使用loadbalancer去调用远程服务。
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
    private final com.apzda.cloud.gsvc.config.GatewayServiceConfigure svcConfigure;
    private final ObjectMapper objectMapper;
    private final int serviceIndex;
    private final WebClient webClient;
    private ReactiveCircuitBreaker circuitBreaker;

    public ReferenceServiceProxy(ReferenceServiceFactoryBean serviceProxyFactoryBean) {
        ApplicationContext applicationContext = serviceProxyFactoryBean.getApplicationContext();
        webClient = applicationContext.getBean(WebClient.class);
        serviceName = serviceProxyFactoryBean.getId();
        appName = serviceProxyFactoryBean.getAppName();
        serviceIndex = serviceProxyFactoryBean.getIndex();
        objectMapper = ResponseUtils.OBJECT_MAPPER;

        lbFunction = applicationContext.getBean(ReactorLoadBalancerExchangeFilterFunction.class);
        svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        serviceConfig = svcConfigure.getServiceConfig(serviceProxyFactoryBean.getIndex());

        log.info("For Service {}@{} Will try picking an instance via load-balancing: http://{}",
                 serviceName,
                 appName,
                 appName);

        url = "http://" + appName;

        if (serviceConfig.isCircuitBreakerEnabled()) {
            val circuitBreakerFactory = applicationContext.getBean(ReactiveResilience4JCircuitBreakerFactory.class);
            circuitBreaker = circuitBreakerFactory.create(serviceName, appName);
            log.debug("Service's circuitBreaker config, service name: {}, group: {}", serviceName, appName);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String requestId = GsvcContextHolder.getRequestId();
        val methodName = method.getName();
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(appName, serviceName, methodName);
        if (serviceMethod == null) {
            if ("toString".equals(methodName)) {
                return toString();
            }
            log.warn("{}@{}/{} method not found", serviceName, appName, methodName);
            throw new NoSuchMethodException(String.format("%s@%s/%s", serviceName, appName, methodName));
        }
        // todo: support grpc
        return doHttpCall(methodName, args[0], serviceMethod);
    }

    private Object doHttpCall(String methodName, Object request, GatewayServiceRegistry.MethodInfo methodInfo) {
        String uri = "/" + appName + "/" + serviceName + "/" + methodName;
        Class<?> rClass = methodInfo.getReturnType();
        val type = methodInfo.getType();
        log.debug("[{}] Starting {} RPC to service: svc://{}", GsvcContextHolder.getRequestId(), type, uri);
        var req = webClient.post().uri(url + uri).accept(MediaType.APPLICATION_JSON);
        var reqBody = prepareRequestBody(req, request);

        if (type == MethodDescriptor.MethodType.UNARY) {
            return doBlockCall(reqBody, methodName, uri, rClass);
        } else {
            return doAsyncCall(reqBody, methodName, uri, rClass);
        }
    }

    private Mono<Object> doAsyncCall(Mono<String> reqBody, String methodName, String uri, Class<?> rClass) {
        //bookmark: async rpc
        var reqMono = reqBody.handle((res, sink) -> {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Response from svc://{}: {}",
                          GsvcContextHolder.getRequestId(),
                          uri,
                          StringUtils.truncate(res, 256));
            }
            sink.next(ResponseUtils.parseResponse(res, rClass));
            sink.complete();
        }).timeout(svcConfigure.getReadTimeout(serviceIndex, methodName));

        if (circuitBreaker != null) {
            //bookmark: 熔断处理
            reqMono = reqMono.transform(it -> circuitBreaker.run(it, Mono::error));
        }
        return handleRpcFallback(reqMono, uri, rClass);
    }

    private Object doBlockCall(Mono<String> reqBody, String methodName, String uri, Class<?> rClass) {
        //bookmark: block rpc
        reqBody = reqBody.timeout(svcConfigure.getReadTimeout(serviceIndex, methodName));

        if (circuitBreaker != null) {
            //BOOKMARK 熔断处理
            reqBody = reqBody.transform(it -> circuitBreaker.run(it, Mono::error));
        }
        val res = (String) handleRpcFallback(reqBody, uri).block();
        if (log.isDebugEnabled()) {
            log.debug("[{}] Response from svc://{}: {}",
                      GsvcContextHolder.getRequestId(),
                      uri,
                      StringUtils.truncate(res, 256));
        }

        return ResponseUtils.parseResponse(res, rClass);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Mono handleRpcFallback(Mono reqBody, String uri) {
        return handleRpcFallback(reqBody, uri, null);
    }

    private Mono<Object> handleRpcFallback(Mono<Object> reqBody, String uri, Class<?> rClass) {
        //bookmark: fallback
        val requestId = GsvcContextHolder.getRequestId();
        return reqBody
            .doOnError(err -> {
                if (log.isTraceEnabled()) {
                    log.error("[{}] RPC failed on svc://{}: ", requestId, uri, err);
                } else {
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

    @SuppressWarnings("unchecked")
    private Mono<String> prepareRequestBody(WebClient.RequestBodySpec req, Object requestObj) {
        val headers = GsvcContextHolder.headers("x-gh-");

        val requestId = GsvcContextHolder.getRequestId();
        headers.put("X-Request-Id", requestId);
        if (StpUtil.isLogin()) {
            //bookmark 透传sa-token登录信息
            val tokenInfo = StpUtil.getTokenInfo();
            log.trace("[{}] Set Sa-Token header: '{}: {}'",
                      requestId,
                      tokenInfo.tokenName,
                      tokenInfo.tokenValue);
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
                                           log.error("[{}] Bad response: {}",
                                                     GsvcContextHolder.getRequestId(),
                                                     e.getMessage());
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
            .append("app", appName)
            .append("service", serviceName)
            .append("index", serviceIndex)
            .append("cb", circuitBreaker != null)
            .toString();
    }
}
