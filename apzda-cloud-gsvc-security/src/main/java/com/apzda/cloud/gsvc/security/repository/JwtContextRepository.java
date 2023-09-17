package com.apzda.cloud.gsvc.security.repository;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.TokenManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * @author fengz
 */
@Slf4j
public class JwtContextRepository implements SecurityContextRepository {

    private static final String CONTEXT_ATTR_NAME = "GSVC.SECURITY.CONTEXT";

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    private final TokenManager tokenManager;

    public JwtContextRepository(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        // 此方法是一个会被延时加载的方法。
        log.warn("[{}] JwtContextRepository loadContext", GsvcContextHolder.getRequestId());
        return getContext(requestResponseHolder.getRequest());
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // 登录/退出成功之后被调用或手动调用
        log.warn("[{}] JwtContextRepository saveContext: {}", GsvcContextHolder.getRequestId(), context);
        tokenManager.save(context.getAuthentication(), request);
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        // 不包括Context时，会触发loadContext()。
        log.warn("[{}] JwtContextRepository containsContext", GsvcContextHolder.getRequestId());
        val storedContext = request.getAttribute(CONTEXT_ATTR_NAME);
        return storedContext != null;
    }

    private SecurityContext getContext(HttpServletRequest request) {
        val storedContext = request.getAttribute(CONTEXT_ATTR_NAME);

        if (storedContext != null) {
            return (SecurityContext) storedContext;
        }

        val context = securityContextHolderStrategy.createEmptyContext();
        try {
            val token = tokenManager.restoreAuthentication(request);
            if (token != null) {
                context.setAuthentication(token);
                request.setAttribute(CONTEXT_ATTR_NAME, context);
                return context;
            }
        }
        catch (Exception e) {
            log.error("[{}] Cannot load context", GsvcContextHolder.getRequestId(), e);
        }
        return null;
    }

}
