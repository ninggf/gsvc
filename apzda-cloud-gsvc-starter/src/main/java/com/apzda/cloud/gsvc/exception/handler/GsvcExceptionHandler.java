package com.apzda.cloud.gsvc.exception.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.apzda.cloud.gsvc.ServiceError;
import com.apzda.cloud.gsvc.core.SaTokenExtendProperties;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.exception.GsvcException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Global Exception handler.
 *
 * @author ninggf windywany@gmail.com
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GsvcExceptionHandler {

    private final SaTokenExtendProperties properties;

    private final ObjectProvider<List<HttpMessageConverter<?>>> httpMessageConverters;

    public ServerResponse handle(Throwable e, ServerRequest request) {
        if (e instanceof NotLoginException) {
            return checkLoginRedirect(request, e);
        }
        else if (e instanceof HttpStatusCodeException codeException
                && codeException.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return checkLoginRedirect(request, e);
        }
        else if (e instanceof HttpStatusCodeException codeException) {
            return ServerResponse.status(codeException.getStatusCode()).body(handle(e));
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception error, HttpServletRequest request) {
        val serverRequest = ServerRequest.create(request, httpMessageConverters.getIfAvailable(Collections::emptyList));
        val accepts = serverRequest.headers().accept();
        val textType = MediaType.parseMediaType("text/*");

        if (isCompatibleWith(textType, accepts)) {
            val loginUrl = properties.getLoginUrl();
            if (loginUrl != null) {
                if (error instanceof NotLoginException || (error instanceof HttpStatusCodeException codeException
                        && codeException.getStatusCode() == HttpStatus.UNAUTHORIZED)
                ) {
                    return ResponseEntity.status(HttpStatus.FOUND).location(loginUrl).build();
                }
            }
            return null;
        }
        else if (error instanceof HttpStatusCodeException codeException) {
            return ResponseEntity.status(codeException.getStatusCode()).body(handle(error));
        }
        else if (error instanceof HttpRequestMethodNotSupportedException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(handle(error));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(error));
    }

    public Response<Void> handle(Throwable e) {
        if (e instanceof NotLoginException notLoginException) {
            int errCode = 10700 - notLoginException.getCode();
            return Response.error(errCode, notLoginException.getMessage());
        }
        else if (e instanceof NotPermissionException) {
            return Response.error(ServiceError.REMOTE_SERVICE_FORBIDDEN);
        }
        else if (e instanceof GsvcException gsvcException) {
            val error = gsvcException.getError();
            return Response.error(error);
        }
        else if (e instanceof HttpRequestMethodNotSupportedException) {
            return Response.error(ServiceError.METHOD_NOT_ALLOWED);
        }
        else {
            return Response.error(ServiceError.SERVICE_ERROR);
        }
    }

    private ServerResponse checkLoginRedirect(ServerRequest request, Throwable e) {
        val loginUrl = getLoginUrl(request.headers().accept());
        if (loginUrl != null) {
            return ServerResponse.status(HttpStatus.FOUND).location(loginUrl).build();
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(e));
    }

    public URI getLoginUrl(List<MediaType> contentTypes) {
        val loginUrl = properties.getLoginUrl();
        val textType = MediaType.parseMediaType("text/*");

        if (loginUrl != null && isCompatibleWith(textType, contentTypes)) {
            return loginUrl;
        }

        return null;
    }

    public static boolean isCompatibleWith(MediaType mediaType, List<MediaType> mediaTypes) {
        if (mediaType != null && !CollectionUtils.isEmpty(mediaTypes)) {
            for (MediaType contentType : mediaTypes) {
                if (contentType.isCompatibleWith(mediaType)) {
                    return true;
                }
            }
        }
        return false;
    }

}
