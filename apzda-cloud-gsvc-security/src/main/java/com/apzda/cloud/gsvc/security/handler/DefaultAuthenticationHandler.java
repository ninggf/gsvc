package com.apzda.cloud.gsvc.security.handler;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import java.io.IOException;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAuthenticationHandler implements AuthenticationHandler {

    private final SecurityConfigProperties properties;

    private final TokenManager tokenManager;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace("[{}] on Authentication Success: {}", GsvcContextHolder.getRequestId(), authentication);
        }

        if (authentication instanceof JwtAuthenticationToken authenticationToken) {
            try {
                val jwtToken = tokenManager.createJwtToken(authentication);
                val cookieCfg = properties.getCookie();
                val cookieName = cookieCfg.getCookieName();

                if (StringUtils.isNoneBlank(cookieName)) {
                    val accessToken = jwtToken.getAccessToken();
                    val cookie = new Cookie(cookieName, accessToken);
                    cookie.setDomain(cookieCfg.getCookieDomain());
                    cookie.setHttpOnly(true);
                    cookie.setSecure(cookieCfg.isCookieSecurity());
                    cookie.setPath(cookieCfg.getCookiePath());
                    cookie.setMaxAge(cookieCfg.getMaxAge());
                    cookie.setAttribute("SameSite", cookieCfg.getSameSite().attributeValue());
                    response.addCookie(cookie);
                }

                authenticationToken.login(jwtToken);

                ResponseUtils.respond(request, response, Response.success(jwtToken));
            }
            catch (Exception e) {
                log.error("Create token failed: {}", e.getMessage(), e);

                ResponseUtils.respond(request, response,
                        Response.error(ServiceError.SERVICE_UNAVAILABLE.code, e.getMessage()));
            }
        }
        else {
            log.error("[{}] Authentication is not a JwtAuthenticationToken instance!",
                    GsvcContextHolder.getRequestId());
            ResponseUtils.respond(request, response, Response.error(ServiceError.INVALID_PRINCIPAL_TYPE));
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace("[{}] on Authentication Failure: {}", GsvcContextHolder.getRequestId(), exception.toString());
        }
        ResponseUtils.respond(request, response,
                Response.error(ServiceError.UNAUTHORIZED.code, exception.getMessage()));
    }

    @Override
    public void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace("[{}] on Access Denied: {}", GsvcContextHolder.getRequestId(),
                    accessDeniedException.getMessage());
        }
        if (!response.isCommitted()) {
            ResponseUtils.respond(request, response, Response.error(ServiceError.FORBIDDEN));
        }
        else {
            throw accessDeniedException;
        }
    }

    @Override
    public void onUnauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace("[{}] on Unauthorized: {}", GsvcContextHolder.getRequestId(), authException.getMessage());
        }
        if (!response.isCommitted()) {
            ResponseUtils.respond(request, response, Response.error(ServiceError.UNAUTHORIZED));
        }
        else {
            throw authException;
        }
    }

    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) throws SessionAuthenticationException {
        if (log.isTraceEnabled()) {
            log.trace("[{}] on Authentication Do Session check: {}", GsvcContextHolder.getRequestId(), authentication);
        }
        // note: run before onAuthenticationSuccess
        tokenManager.verify(authentication);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("[{}] logout: {}", GsvcContextHolder.getRequestId(), authentication);
            }
            if (authentication instanceof JwtAuthenticationToken auth) {
                auth.logout();
            }
            this.tokenManager.remove(authentication, request);
        }
        catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Token Manager cannot remove authentication data: {}", authentication, e);
            }
        }
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace("[{}] on Logout Success: {}", GsvcContextHolder.getRequestId(), authentication);
        }

        val mediaTypes = ResponseUtils.mediaTypes(request);
        val homePage = ResponseUtils.getHomePage(mediaTypes);
        val compatibleWith = ResponseUtils.isCompatibleWith(ResponseUtils.TEXT_MASK, mediaTypes);

        if (compatibleWith != null && StringUtils.isNotBlank(homePage)) {
            log.trace("[{}] on Logout Success and redirect to: {}", GsvcContextHolder.getRequestId(), homePage);
            response
                .setContentType(compatibleWith.isConcrete() ? compatibleWith.toString() : MediaType.TEXT_PLAIN_VALUE);
            response.sendRedirect(homePage);
        }
        else {
            log.trace("[{}] on Logout Success with json data", GsvcContextHolder.getRequestId());
            ResponseUtils.respond(request, response, Response.success("Logout"));
        }
    }

}
