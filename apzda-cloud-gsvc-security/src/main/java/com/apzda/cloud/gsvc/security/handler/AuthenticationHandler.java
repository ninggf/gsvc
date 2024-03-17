package com.apzda.cloud.gsvc.security.handler;

import com.apzda.cloud.gsvc.IServiceError;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.security.exception.AuthenticationError;
import com.apzda.cloud.gsvc.security.exception.InvalidSessionException;
import com.apzda.cloud.gsvc.security.exception.UnRealAuthenticatedException;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
public interface AuthenticationHandler extends
    AuthenticationFailureHandler,
    AuthenticationSuccessHandler,
    AccessDeniedHandler,
    AuthenticationEntryPoint,
    SessionAuthenticationStrategy,
    InvalidSessionStrategy,
    LogoutHandler,
    LogoutSuccessHandler {
    Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);

    @Override
    default void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {
        onAccessDenied(request, response, accessDeniedException);
    }

    @Override
    default void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        logger.trace("[{}] Need User provided his/her Credentials by an entryPoint", GsvcContextHolder.getRequestId());
        onUnauthorized(request, response, authException);
    }

    @Override
    default void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        logger.trace("[{}] InvalidSessionDetected", GsvcContextHolder.getRequestId());
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

    static void handleAuthenticationException(HttpServletRequest request, HttpServletResponse response,
                                              @NonNull AuthenticationException exception) throws IOException {
        logger.trace("[{}] Authentication Exception caught: ", GsvcContextHolder.getRequestId(), exception);
        if (!response.isCommitted()) {
            if (exception instanceof UsernameNotFoundException) {
                val error = Response.error(ServiceError.USER_PWD_INCORRECT);
                error.setHttpCode(401);
                ResponseUtils.respond(request, response, error);
            } else if (exception instanceof AuthenticationError authenticationError) {
                val error = Response.error(authenticationError.getError());
                error.setHttpCode(401);
                ResponseUtils.respond(request, response, error);
            } else if (exception instanceof AccountStatusException statusException) {
                handleAccountStatusException(request, response, statusException);
            } else if(exception instanceof InsufficientAuthenticationException) {
                ResponseUtils.respond(request, response, Response.error(ServiceError.UNAUTHORIZED));
            } else {
                ResponseUtils.respond(request, response, Response.error(ServiceError.UNAUTHORIZED));
            }
        } else {
            throw exception;
        }
    }

    static void handleAccountStatusException(HttpServletRequest request, HttpServletResponse response,
                                             @NonNull AccountStatusException exception) throws IOException {
        IServiceError error;
        if (exception instanceof CredentialsExpiredException) {
            error = ServiceError.CREDENTIALS_EXPIRED;
        } else if (exception instanceof AccountExpiredException) {
            error = ServiceError.ACCOUNT_EXPIRED;
        } else if (exception instanceof LockedException) {
            error = ServiceError.ACCOUNT_LOCKED;
        } else if (exception instanceof UnRealAuthenticatedException) {
            error = ServiceError.ACCOUNT_UN_AUTHENTICATED;
        } else {
            error = ServiceError.ACCOUNT_DISABLED;
        }
        val resp = Response.error(error);
        resp.setHttpCode(401);
        ResponseUtils.respond(request, response, resp);
    }
}
