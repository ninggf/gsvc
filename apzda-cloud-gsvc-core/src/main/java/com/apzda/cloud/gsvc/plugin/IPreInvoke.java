package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.servlet.function.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * @author fengz
 */
public interface IPreInvoke extends IPlugin {

    Mono<JsonNode> preInvoke(ServerRequest request, Mono<JsonNode> data, ServiceMethod method);

}
