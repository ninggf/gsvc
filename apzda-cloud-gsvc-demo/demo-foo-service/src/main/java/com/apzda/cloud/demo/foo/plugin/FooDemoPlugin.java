package com.apzda.cloud.demo.foo.plugin;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * @author fengz
 */
@Component
@Slf4j
public class FooDemoPlugin implements IGlobalPlugin, IPreCall, IPreInvoke {

    @Override
    public WebClient.RequestBodySpec preCall(WebClient.RequestBodySpec request, Object data, ServiceMethod method) {
        log.info("[{}] FooDemoPlugin#preCall is working", GsvcContextHolder.getRequestId());
        return request;
    }

    @Override
    public Mono<JsonNode> preInvoke(ServerRequest request, Mono<JsonNode> data, ServiceMethod method) {
        log.info("[{}] FooDemoPlugin#preInvoke is working", GsvcContextHolder.getRequestId());
        return data;
    }

}
