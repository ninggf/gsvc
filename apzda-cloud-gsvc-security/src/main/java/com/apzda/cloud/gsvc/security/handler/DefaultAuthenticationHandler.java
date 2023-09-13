package com.apzda.cloud.gsvc.security.handler;

import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final GsvcExceptionHandler gsvcExceptionHandler;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("onAuthenticationSuccess - 登录成功: {}", authentication);
        // 1. 如果是通过接口方式登录,此时应该返回access-token
        // 2. 如果是网页登录，此时应该重定向到指定的成功页面
        response.getWriter().write("login successfully");
        response.getWriter().close();
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        log.warn("onAuthenticationFailure - 登录失败: {}", exception.toString());
        // 1. 如果是通过接口方式登录,此时应该响应401或具体的错误信息
        // 2. 如果是网页登录，此时应该重定向到指定的失败页面（可能还是登录页，带上错误信息）
        response.setStatus(401);
    }

    @Override
    public void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("onAccessDenied: {}", accessDeniedException.getMessage());
        if (!response.isCommitted()) {
            // 1. 接口 => 403 错误码
            // 2. 网页 => 403
            response.setStatus(403);
        }
        else {
            throw accessDeniedException;
        }
    }

    @Override
    public void onUnauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        log.warn("onUnauthorized: {}", authException.getMessage());
        if (!response.isCommitted()) {
            // 1. 接口 => 401 错误码
            // 2. 网页 => 登录页
            response.setStatus(401);
        }
        else {
            throw authException;
        }
    }

    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) throws SessionAuthenticationException {
        log.warn("检测认证信息，保证认证有效: {}", authentication);
    }

}
