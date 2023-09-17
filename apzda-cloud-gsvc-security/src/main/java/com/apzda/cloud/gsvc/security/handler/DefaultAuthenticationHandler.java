package com.apzda.cloud.gsvc.security.handler;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.security.TokenManager;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
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
        log.trace("Authentication Success: {}", authentication);
        if (authentication instanceof JwtAuthenticationToken) {
            try {
                val jwtToken = tokenManager.createJwtToken(authentication, true);
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

                ResponseUtils.respond(request, response, Response.success(jwtToken));
            }
            catch (Exception e) {
                log.error("Create token failed: {}", e.getMessage(), e);

                ResponseUtils.respond(request, response,
                        Response.error(ServiceError.SERVICE_UNAVAILABLE.code, e.getMessage()));
            }
        }
        else {
            log.error("Authentication is not a JwtAuthenticationToken instance!");
            ResponseUtils.respond(request, response, Response.error(ServiceError.INVALID_PRINCIPAL_TYPE));
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        log.trace("Authentication Failure: {}", exception.toString());
        ResponseUtils.respond(request, response,
                Response.error(ServiceError.UNAUTHORIZED.code, exception.getMessage()));
    }

    @Override
    public void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.trace("onAccessDenied: {}", accessDeniedException.getMessage());
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
        log.trace("onUnauthorized: {}", authException.getMessage());
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
        log.trace("会话检测，保证当前会议中的认证有效: {}", authentication);

        if (authentication instanceof JwtAuthenticationToken authenticationToken) {
            val token = authenticationToken.getJwtToken();
            tokenManager.verify(token, authenticationToken);
        }
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        log.trace("[{}] DefaultAuthenticationHandler onLogoutSuccess", GsvcContextHolder.getRequestId());

        val mediaTypes = ResponseUtils.mediaTypes(request);
        val homePage = ResponseUtils.getHomePage(mediaTypes);
        if (homePage != null) {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.sendRedirect(homePage.toString());
        }
        else {
            ResponseUtils.respond(request, response, Response.success("Logout"));
        }
    }

}