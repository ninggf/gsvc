package com.apzda.cloud.gsvc.security.repository;

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
            log.trace("Start to load SecurityContext");
        }
        val request = requestResponseHolder.getRequest();

        val storedContext = request.getAttribute(CONTEXT_ATTR_NAME);

        if (storedContext != null) {
            if (log.isTraceEnabled()) {
                log.trace("Context Loaded from request attribute");
            }
            return (SecurityContext) storedContext;
        }

        val context = securityContextHolderStrategy.createEmptyContext();

        try {
            val authentication = tokenManager.restoreAuthentication(request);
            if (authentication != null) {
                context.setAuthentication(authentication);
                if (log.isTraceEnabled()) {
                    log.trace("Context loaded from TokenManager: {}", tokenManager);
                }
            }
            else if (log.isTraceEnabled()) {
                log.trace("Cannot loaded Context. the empty context is used");
            }
        }
        catch (AuthenticationException ae) {
            throw ae;
        }
        catch (Exception e) {
            log.error("Error happened while loading Context: {}", e.getMessage());
        }
        request.setAttribute(CONTEXT_ATTR_NAME, context);
        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        try {
            tokenManager.save(context.getAuthentication(), request);
            if (log.isTraceEnabled()) {
                log.trace("SecurityContext saved: {}", context);
            }
        }
        catch (Exception e) {
            log.error("Save Context failed: {} - {}", e.getMessage(), context);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        val containsContext = request.getAttribute(CONTEXT_ATTR_NAME) != null;
        if (log.isTraceEnabled()) {
            log.trace("Contains Context: {}", containsContext);
        }
        return containsContext;
    }

}
