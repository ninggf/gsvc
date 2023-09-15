package com.apzda.cloud.gsvc.security.repository;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.TokenManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Collections;

/**
 * @author fengz
 */
@Slf4j
public class InMemoryRepository implements SecurityContextRepository {

    private static final String CONTEXT_ATTR_NAME = "GSVC.SECURITY.CONTEXT";

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    private final TokenManager tokenManager;

    public InMemoryRepository(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        // 此方法是一个会被延时加载的方法。
        log.warn("[{}] InMemoryRepository loadContext", GsvcContextHolder.getRequestId());
        return getContext(requestResponseHolder.getRequest());
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // 登录成功之后被调用或手动调用
        log.warn("[{}] InMemoryRepository saveContext: {}", GsvcContextHolder.getRequestId(), context);
        tokenManager.saveToken(context.getAuthentication(), request);
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        // 不包括Context时，会触发loadContext()。
        log.warn("[{}] InMemoryRepository containsContext", GsvcContextHolder.getRequestId());
        val storedContext = request.getAttribute(CONTEXT_ATTR_NAME);
        return storedContext != null;
    }

    private SecurityContext getContext(HttpServletRequest request) {
        val authToken = request.getHeader("AUTHORIZATION");
        log.debug("[{}] InMemoryRepository getContext: Token = {}", GsvcContextHolder.getRequestId(), authToken);
        UsernamePasswordAuthenticationToken.authenticated("leo", "hahah", Collections.emptyList());
        if (StringUtils.isBlank(authToken)) {
            return null;
        }

        val storedContext = request.getAttribute(CONTEXT_ATTR_NAME);

        if (storedContext != null) {
            return (SecurityContext) storedContext;
        }
        log.warn("[{}] Load context from JWT", GsvcContextHolder.getRequestId());
        val context = securityContextHolderStrategy.createEmptyContext();

        val token = tokenManager.restore(request);

        if (token != null) {
            context.setAuthentication(token);
            request.setAttribute(CONTEXT_ATTR_NAME, context);
            return context;
        }

        return null;
    }

}
