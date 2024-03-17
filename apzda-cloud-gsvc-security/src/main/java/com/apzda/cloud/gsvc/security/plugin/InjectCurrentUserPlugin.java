/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.plugin;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.resolver.CurrentUserParamResolver;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * @author fengz windywany@gmail.com
 **/
@Slf4j
@RequiredArgsConstructor
public class InjectCurrentUserPlugin implements IGlobalPlugin, IPreInvoke, IPreCall {

    private final SecurityConfigProperties properties;

    @Override
    public WebClient.RequestBodySpec preCall(WebClient.RequestBodySpec request, Object data, ServiceMethod method) {
        val tokenName = properties.getTokenName();
        val cookieName = properties.getCookie().getCookieName();
        if (StringUtils.isNotBlank(tokenName)) {
            val tokenValue = StringUtils.defaultIfBlank(GsvcContextHolder.header(tokenName), GsvcContextHolder.cookie(cookieName));
            if (StringUtils.isNotBlank(tokenValue)) {
                request = request.headers(httpHeaders -> {
                    if (log.isTraceEnabled()) {
                        log.trace("[{}] Transit Header: {}: {}", GsvcContextHolder.getRequestId(), tokenName,
                            tokenValue);
                    }
                    httpHeaders.put(tokenName, Collections.singletonList(tokenValue));
                });
            }
        }

        return request;
    }

    @Override
    public Mono<JsonNode> preInvoke(ServerRequest request, Mono<JsonNode> data, ServiceMethod method) {
        val context = SecurityContextHolder.getContext();
        if (context == null) {
            return data;
        }
        val authentication = context.getAuthentication();

        if (authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated()) {

            data = data.map(d -> {
                val currentUser = CurrentUserParamResolver.getCurrentUserBuilder(authentication).build();

                if (d instanceof ObjectNode objectNode) {
                    if (log.isTraceEnabled()) {
                        log.trace("[{}] Inject CurrentUser: {}", GsvcContextHolder.getRequestId(), currentUser);
                    }
                    objectNode.set("currentUser", new POJONode(currentUser));
                }

                return d;
            });
        }
        return data;
    }

}
