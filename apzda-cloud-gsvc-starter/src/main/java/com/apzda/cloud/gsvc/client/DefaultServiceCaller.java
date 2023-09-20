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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author fengz
 */
@Slf4j
public class DefaultServiceCaller implements IServiceCaller {

    public static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    protected final ObjectMapper objectMapper = ResponseUtils.OBJECT_MAPPER;

    protected final ApplicationContext applicationContext;

    protected final GatewayServiceConfigure svcConfigure;

    public DefaultServiceCaller(ApplicationContext applicationContext, GatewayServiceConfigure svcConfigure) {
        this.applicationContext = applicationContext;
        this.svcConfigure = svcConfigure;
    }

    @Override
    public <T, R> R unaryCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz) {
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(clazz, method);
        val url = serviceMethod.getRpcAddr();
        if (log.isDebugEnabled()) {
            val requestId = GsvcContextHolder.getRequestId();
            log.debug("[{}] Start Block RPC: {}", requestId, url);
        }
        val reqBody = prepareRequestBody(request, serviceMethod);
        return doBlockCall(reqBody.bodyToMono(String.class), serviceMethod, url, resClazz);
    }

    protected <R> R doBlockCall(Mono<String> reqBody, ServiceMethod serviceMethod, String uri, Class<R> rClass) {
        // bookmark: block rpc
        val res = handleRpcFallback(Flux.concat(reqBody), serviceMethod, String.class).blockFirst();

        if (log.isDebugEnabled()) {
            log.debug("[{}] Response from {}: {}", GsvcContextHolder.getRequestId(), uri, res);
        }

        return ResponseUtils.parseResponse(res, rClass);
    }

    @Override
    public <T, R> Flux<R> serverStreamingCall(Class<?> clazz, String method, T request, Class<T> reqClazz,
            Class<R> resClazz) {
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(clazz, method);
        val url = serviceMethod.getRpcAddr();

        if (log.isDebugEnabled()) {
            val requestId = GsvcContextHolder.getRequestId();
            log.debug("[{}] Start Async RPC: {}", requestId, url);
        }
        val reqBody = prepareRequestBody(request, serviceMethod);

        return doAsyncCall(reqBody.bodyToFlux(SSE_RESPONSE_TYPE), serviceMethod, url, resClazz);
    }

    protected <R> Flux<R> doAsyncCall(Flux<ServerSentEvent<String>> reqBody, ServiceMethod serviceMethod, String uri,
            Class<R> rClass) {
        // bookmark: async rpc
        val requestId = GsvcContextHolder.getRequestId();
        var reqMono = reqBody.map(res -> {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Response from {}: {}", requestId, uri, res);
            }
            try {
                val data = res.data();
                return ResponseUtils.parseResponse(data, rClass);
            }
            catch (Exception e) {
                return ResponseUtils.fallback(e, serviceMethod.getServiceName(), rClass);
            }
        });

        return handleRpcFallback(reqMono, serviceMethod, rClass);
    }

    protected <R> Flux<R> handleRpcFallback(Flux<R> reqBody, ServiceMethod method, Class<R> rClass) {
        // bookmark: fallback
        val uri = method.getRpcAddr();
        val plugins = method.getPlugins();
        var size = plugins.size();

        reqBody = reqBody.doOnError(err -> {
            val requestId = GsvcContextHolder.getRequestId();
            log.error("[{}] RPC({}) failed: {}", requestId, uri, err.getMessage());
        });

        while (--size >= 0) {
            val plugin = plugins.get(size);
            if (plugin instanceof IPostCall postPlugin) {
                reqBody = postPlugin.postCall(reqBody, method, rClass);
            }
        }

        return reqBody.contextCapture();
    }

    protected WebClient.ResponseSpec prepareRequestBody(Object requestObj, ServiceMethod method) {
        var url = method.getRpcAddr();
        val webClient = applicationContext.getBean(method.getClientBeanName(), WebClient.class);

        WebClient.RequestBodySpec req = webClient.post().uri(url).accept(MediaType.APPLICATION_JSON);
        List<IPlugin> plugins = method.getPlugins();

        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreCall prePlugin) {
                req = prePlugin.preCall(req, requestObj, method);
            }
        }

        try {
            val requestBody = objectMapper.writeValueAsString(requestObj);
            val request = req.contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .bodyValue(requestBody);

            return request.retrieve();
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
