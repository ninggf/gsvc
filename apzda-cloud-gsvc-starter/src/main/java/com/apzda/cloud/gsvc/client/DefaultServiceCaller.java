package com.apzda.cloud.gsvc.client;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.IPostCall;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author fengz
 */
@Slf4j
public class DefaultServiceCaller implements IServiceCaller {

    protected final ObjectMapper objectMapper = ResponseUtils.OBJECT_MAPPER;

    protected final ApplicationContext applicationContext;

    protected final GatewayServiceConfigure svcConfigure;

    public DefaultServiceCaller(ApplicationContext applicationContext, GatewayServiceConfigure svcConfigure) {
        this.applicationContext = applicationContext;
        this.svcConfigure = svcConfigure;
    }

    @Override
    public <T, R> R unaryCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz) {
        val requestId = GsvcContextHolder.getRequestId();
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(clazz, method);
        val url = serviceMethod.getRpcAddr();
        log.debug("[{}] Start Block RPC: {}", requestId, url);
        val reqBody = prepareRequestBody(request, serviceMethod);
        return doBlockCall(reqBody, serviceMethod, url, resClazz);
    }

    @Override
    public <T, R> Mono<R> serverStreamingCall(Class<?> clazz, String method, T request, Class<T> reqClazz,
            Class<R> resClazz) {
        return bidiStreamingCall(clazz, method, Mono.just(request), reqClazz, resClazz);
    }

    @Override
    public <T, R> Mono<R> bidiStreamingCall(Class<?> clazz, String method, Mono<T> request, Class<T> reqClazz,
            Class<R> resClazz) {
        val requestId = GsvcContextHolder.getRequestId();
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(clazz, method);
        val url = serviceMethod.getRpcAddr();
        log.debug("[{}] Start Async RPC: {}", requestId, url);
        val reqBody = prepareRequestBody(request, serviceMethod);

        return doAsyncCall(reqBody, serviceMethod, url, resClazz);
    }

    protected <R> R doBlockCall(Mono<String> reqBody, ServiceMethod serviceMethod, String uri, Class<R> rClass) {
        val cfgName = serviceMethod.getCfgName();
        val methodName = serviceMethod.getDmName();
        // bookmark: block rpc
        val res = handleRpcFallback(reqBody, serviceMethod, String.class).block();

        if (log.isDebugEnabled()) {
            log.debug("[{}] Response from {}: {}", GsvcContextHolder.getRequestId(), uri, res);
        }

        return ResponseUtils.parseResponse(res, rClass);
    }

    protected <R> Mono<R> doAsyncCall(Mono<String> reqBody, ServiceMethod serviceMethod, String uri, Class<R> rClass) {
        val cfgName = serviceMethod.getCfgName();
        val methodName = serviceMethod.getDmName();
        // bookmark: async rpc
        var reqMono = reqBody.<R>handle((res, sink) -> {
            if (log.isDebugEnabled()) {
                val requestId = GsvcContextHolder.getRequestId();
                log.debug("[{}] Response from {}: {}", requestId, uri, res);
            }
            // 这里使用到了GsvcContextHolder, 不知道使用contextCapture()是否有用!
            sink.next(ResponseUtils.parseResponse(res, rClass));
            sink.complete();
        });

        return handleRpcFallback(reqMono, serviceMethod, rClass);
    }

    protected <R> Mono<R> handleRpcFallback(Mono<R> reqBody, ServiceMethod method, Class<R> rClass) {
        // bookmark: fallback
        val uri = method.getRpcAddr();
        val requestId = GsvcContextHolder.getRequestId();
        val plugins = method.getPlugins();
        var size = plugins.size();

        reqBody = reqBody.doOnError(err -> {
            log.error("[{}] RPC({}) failed: {}", requestId, uri, err.getMessage());
        });

        while (--size >= 0) {
            val plugin = plugins.get(size);
            if (plugin instanceof IPostCall postPlugin) {
                reqBody = postPlugin.postCall(reqBody, method, rClass);
            }
        }
        // tbd: fallback or throw exception?
        // onErrorResume(e -> Mono.just(ResponseUtils.fallback(e,serviceName,rClass)));
        return reqBody.contextCapture();
    }

    @SuppressWarnings("unchecked")
    protected Mono<String> prepareRequestBody(Object requestObj, ServiceMethod method) {
        val requestId = GsvcContextHolder.getRequestId();
        var url = method.getRpcAddr();
        val webClient = applicationContext.getBean(method.getClientBeanName(), WebClient.class);

        WebClient.RequestBodySpec req = webClient.post().uri(url).accept(MediaType.APPLICATION_JSON);
        List<IPlugin> plugins = method.getPlugins();

        if (!(requestObj instanceof Mono)) {
            requestObj = Mono.just(requestObj);
        }

        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreCall prePlugin) {
                req = prePlugin.preCall(req, (Mono<Object>) requestObj, method);
            }
        }

        val request = req.contentType(MediaType.APPLICATION_JSON)
            .acceptCharset(StandardCharsets.UTF_8)
            .body(BodyInserters.fromPublisher(((Mono<Object>) requestObj).handle((obj, sink) -> {
                try {
                    sink.next(objectMapper.writeValueAsString(obj));
                    sink.complete();
                }
                catch (JsonProcessingException e) {
                    log.error("[{}] Bad Request for {}: {} - {}", requestId, url, e.getMessage(), obj);
                    sink.error(e);
                }
            }), String.class));

        return request.retrieve().bodyToMono(String.class);
    }

}
