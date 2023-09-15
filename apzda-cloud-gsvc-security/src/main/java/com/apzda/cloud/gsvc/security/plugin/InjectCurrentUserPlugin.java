/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.apzda.cloud.gsvc.security.token.AuthenticationToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * @author fengz windywany@gmail.com
 **/
@Slf4j
public class InjectCurrentUserPlugin implements IGlobalPlugin, IPreInvoke, IPreCall {

    // private final ObjectMapper objectMapper;
    @Override
    public WebClient.RequestBodySpec preCall(WebClient.RequestBodySpec request, Mono<Object> data,
            ServiceMethod method) {

        return request;
    }

    @Override
    public Mono<JsonNode> preInvoke(ServerRequest request, Mono<JsonNode> data, ServiceMethod method) {
        val context = SecurityContextHolder.getContext();
        val authentication = context.getAuthentication();
        if (authentication.isAuthenticated() && authentication instanceof AuthenticationToken authenticationToken) {
            val jwtToken = authenticationToken.getJwtToken();

            data = data.map(d -> {

                val currentUser = GsvcExt.CurrentUser.newBuilder()
                    .setUid(jwtToken.getUid())
                    .setName(jwtToken.getName())
                    .build();

                if (d instanceof ObjectNode objectNode) {
                    objectNode.set("currentUser", new POJONode(currentUser));
                }

                return d;
            });
        }
        return data;
    }

}
