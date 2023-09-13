package com.apzda.cloud.gsvc.security.handler;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSigner;
import com.apzda.cloud.gsvc.config.GlobalConfig;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.security.IUser;
import com.apzda.cloud.gsvc.security.JwtToken;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import java.io.IOException;
import java.util.UUID;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAuthenticationHandler implements AuthenticationHandler {

    private final GlobalConfig config;

    private final SecurityConfigProperties properties;

    private final JWTSigner jwtSigner;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("onAuthenticationSuccess - 登录成功: {}", authentication);

        val principal = authentication.getPrincipal();

        if (principal instanceof IUser user) {
            val token = JWT.create();
            token.setPayload("i", user.getUid());
            token.setSubject(authentication.getName());
            token.setSigner(jwtSigner);
            val accessToken = token.sign();
            var refreshToken = UUID.randomUUID().toString();

            val jwtToken = JwtToken.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .uid(user.getUid())
                .name(authentication.getName())
                .build();

            val cookieCfg = properties.getCookie();
            val cookieName = cookieCfg.getCookieName();

            if (StringUtils.isNoneBlank(cookieName)) {
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
        else {
            log.error("Principal is not a IUser instance!");
            ResponseUtils.respond(request, response, Response.error(ServiceError.INVALID_PRINCIPAL_TYPE));
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        log.warn("onAuthenticationFailure - 登录失败: {}", exception.toString());
        // 1. 如果是通过接口方式登录,此时应该响应401或具体的错误信息
        val loginPage = config.getLoginPage();
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
