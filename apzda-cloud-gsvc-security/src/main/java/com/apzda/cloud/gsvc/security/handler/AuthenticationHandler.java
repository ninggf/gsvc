package com.apzda.cloud.gsvc.security.handler;

import com.apzda.cloud.gsvc.security.exception.InvalidSessionException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.session.InvalidSessionStrategy;

import java.io.IOException;

/**
 * @author fengz
 */
public interface AuthenticationHandler extends AuthenticationFailureHandler, AuthenticationSuccessHandler,
        AccessDeniedHandler, AuthenticationEntryPoint, SessionAuthenticationStrategy, InvalidSessionStrategy,
        LogoutHandler, LogoutSuccessHandler {

    @Override
    default void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        onAccessDenied(request, response, accessDeniedException);
    }

    @Override
    default void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        onUnauthorized(request, response, authException);
    }

    @Override
    default void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        onUnauthorized(request, response, new InvalidSessionException("Invalid Session"));
    }

    void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException;

    void onUnauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException;

    @Override
    default void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

    }

    @Override
    default void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

    }

}
