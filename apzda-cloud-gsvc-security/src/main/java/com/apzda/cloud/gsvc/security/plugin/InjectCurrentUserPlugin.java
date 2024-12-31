/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.plugin;

import cn.hutool.core.util.StrUtil;
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
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
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
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    @NonNull
    public WebClient.RequestBodySpec preCall(@NonNull WebClient.RequestBodySpec request, @Nullable Object data,
            @Nullable ServiceMethod method) {
        val context = SecurityContextHolder.getContext();
        if (context == null) {
            return request;
        }
        val authentication = context.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return request;
        }
        if (!(authentication instanceof JwtAuthenticationToken)) {
            return request;
        }
        val jwtToken = ((JwtAuthenticationToken) authentication).getJwtToken();
        val bearer = StringUtils.defaultIfBlank(properties.getBearer(), "");
        val accessToken = jwtToken.getAccessToken();
        val tokenName = properties.getTokenName();

        if (StringUtils.isNotBlank(tokenName) && StringUtils.isNotBlank(accessToken)) {
            request = request.headers(httpHeaders -> {
                if (log.isTraceEnabled()) {
                    log.trace("Transit Header: {}: {}", tokenName, accessToken);
                }
                httpHeaders.put(tokenName, Collections
                    .singletonList(StringUtils.strip(StrUtil.format("{} {}", bearer, jwtToken.getAccessToken()))));
            });
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
                if (d instanceof ObjectNode objectNode) {
                    val currentUser = CurrentUserParamResolver.getCurrentUserBuilder(authentication).build();
                    if (log.isTraceEnabled()) {
                        log.trace("Inject CurrentUser: {}", currentUser);
                    }
                    objectNode.set("currentUser", new POJONode(currentUser));
                }

                return d;
            });
        }
        return data;
    }

}
