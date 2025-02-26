package com.apzda.cloud.gsvc.security.handler;

import com.apzda.cloud.gsvc.IServiceError;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.security.exception.AuthenticationError;
import com.apzda.cloud.gsvc.security.exception.InvalidSessionException;
import com.apzda.cloud.gsvc.security.exception.UnRealAuthenticatedException;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
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

import static com.apzda.cloud.gsvc.security.repository.JwtContextRepository.CONTEXT_ATTR_EXCEPTION;

/**
 * @author fengz
 */
public interface AuthenticationHandler extends AuthenticationFailureHandler, AuthenticationSuccessHandler,
        AccessDeniedHandler, AuthenticationEntryPoint, SessionAuthenticationStrategy, InvalidSessionStrategy,
        LogoutHandler, LogoutSuccessHandler {

    Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);

    @Override
    default void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        onAccessDenied(request, response, accessDeniedException);
    }

    @Override
    default void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        val exception = request.getAttribute(CONTEXT_ATTR_EXCEPTION);
        if (exception != null) {
            onUnauthorized(request, response, (AuthenticationException) exception);
        }
        else {
            onUnauthorized(request, response, authException);
        }
    }

    @Override
    default void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        logger.trace("InvalidSessionDetected");
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

    static void handleAuthenticationException(@Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response, @Nonnull Exception exception) throws IOException {
        if (!response.isCommitted()) {
            logger.trace("Authentication Exception caught and handled: {}", exception.getMessage());
            val error = getAuthenticationError(exception);
            ResponseUtils.respond(request, response, error);
        }
        else {
            logger.error("Authentication Exception cannot be handled for response which was commited", exception);
        }
    }

    @Nonnull
    static Response<?> getAuthenticationError(@Nonnull Exception exception) {
        if (exception instanceof UsernameNotFoundException) {
            val error = Response.error(ServiceError.USER_PWD_INCORRECT);
            error.setHttpCode(403);
            return error;
        }
        else if (exception instanceof AuthenticationError authenticationError) {
            val err = authenticationError.getError();
            val error = Response.error(err);
            val httpCode = err.httpCode();
            error.setHttpCode(httpCode > 0 ? httpCode : 403);
            return error;
        }
        else if (exception instanceof AccountStatusException statusException) {
            return handleAccountStatusException(statusException);
        }
        else if (exception instanceof GsvcException gsvcException) {
            val error = Response.error(gsvcException.getError());
            error.setHttpCode(403);
            return error;
        }
        else {
            val error = Response.error(ServiceError.UNAUTHORIZED);
            error.setHttpCode(401);
            return error;
        }
    }

    @Nonnull
    static Response<?> handleAccountStatusException(@Nonnull AccountStatusException exception) {
        IServiceError error;
        if (exception instanceof CredentialsExpiredException) {
            error = ServiceError.CREDENTIALS_EXPIRED;
        }
        else if (exception instanceof AccountExpiredException) {
            error = ServiceError.ACCOUNT_EXPIRED;
        }
        else if (exception instanceof LockedException) {
            error = ServiceError.ACCOUNT_LOCKED;
        }
        else if (exception instanceof UnRealAuthenticatedException) {
            error = ServiceError.ACCOUNT_UN_AUTHENTICATED;
        }
        else {
            error = ServiceError.ACCOUNT_DISABLED;
        }
        val resp = Response.error(error);
        resp.setHttpCode(403);
        return resp;
    }

}
