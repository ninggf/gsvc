package com.apzda.cloud.gsvc.security.repository;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.AuthenticationException;
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

    private static final String CONTEXT_ATTR_LOADING = "GSVC.SECURITY.LOADING";

    private static final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    private final TokenManager tokenManager;

    public JwtContextRepository(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        log.trace("Start loading SecurityContext");
        val request = requestResponseHolder.getRequest();
        val gsvcContext = request.getAttribute("GSVC.CONTEXT");
        if (gsvcContext instanceof GsvcContextHolder.GsvcContext gContext) {
            gContext.restore();
        }

        val storedContext = request.getAttribute(CONTEXT_ATTR_NAME);
        if (storedContext != null) {
            log.trace("Context Loaded from request attribute");
            return (SecurityContext) storedContext;
        }

        val context = securityContextHolderStrategy.createEmptyContext();

        if (request.getAttribute(CONTEXT_ATTR_LOADING) != null) {
            log.warn("Dead lock while loading Security context! ");
            return context;
        }

        request.setAttribute(CONTEXT_ATTR_LOADING, Boolean.TRUE);

        try {
            val authentication = tokenManager.restoreAuthentication(request);
            if (authentication != null) {
                context.setAuthentication(authentication);
                log.trace("Context loaded from TokenManager: {}", tokenManager);
            }
        }
        catch (AuthenticationException ignored) {
        }
        catch (Exception e) {
            log.error("Error happened while loading Context: {}", e.getMessage());
        }
        request.setAttribute(CONTEXT_ATTR_NAME, context);
        request.removeAttribute(CONTEXT_ATTR_LOADING);
        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        try {
            tokenManager.save(context.getAuthentication(), request);
            log.trace("SecurityContext saved: {}", context);
        }
        catch (Exception e) {
            log.error("Save Context failed: {} - {}", e.getMessage(), context);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        val containsContext = request.getAttribute(CONTEXT_ATTR_NAME) != null;
        log.trace("SecurityContext is loaded: {}", containsContext);
        return containsContext;
    }

}
