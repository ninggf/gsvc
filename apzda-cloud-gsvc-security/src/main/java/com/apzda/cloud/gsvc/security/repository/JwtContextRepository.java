package com.apzda.cloud.gsvc.security.repository;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.token.TokenManager;
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
        if (log.isTraceEnabled()) {
            log.trace("[{}] Start to load Context", GsvcContextHolder.getRequestId());
        }
        val request = requestResponseHolder.getRequest();

        val storedContext = request.getAttribute(CONTEXT_ATTR_NAME);

        if (storedContext != null) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Loaded Context from request attribute", GsvcContextHolder.getRequestId());
            }
            return (SecurityContext) storedContext;
        }

        val context = securityContextHolderStrategy.createEmptyContext();
        // context.setAuthentication(JwtAuthenticationToken.unauthenticated("anonymous",
        // ""));

        try {
            val authentication = tokenManager.restoreAuthentication(request);
            if (authentication != null) {
                context.setAuthentication(authentication);
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Loading Context by {}", GsvcContextHolder.getRequestId(), tokenManager);
                }
            }
            else if (log.isTraceEnabled()) {
                log.trace("[{}] Cannot Restore Authentication", GsvcContextHolder.getRequestId());
            }
        }
        catch (Exception e) {
            log.error("[{}] Cannot Restore Authentication: {}", GsvcContextHolder.getRequestId(), e.getMessage());
        }
        request.setAttribute(CONTEXT_ATTR_NAME, context);
        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        try {
            tokenManager.save(context.getAuthentication(), request);
            if (log.isTraceEnabled()) {
                log.trace("[{}] Context saved: {}", GsvcContextHolder.getRequestId(), context);
            }
        }
        catch (Exception e) {
            log.error("[{}]  Save Context failed: {} - {}", GsvcContextHolder.getRequestId(), e.getMessage(), context);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        val containsContext = request.getAttribute(CONTEXT_ATTR_NAME) != null;
        if (log.isTraceEnabled()) {
            log.trace("[{}] Contains Context: {}", GsvcContextHolder.getRequestId(), containsContext);
        }
        return containsContext;
    }

}
