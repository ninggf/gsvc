package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.config.SaTokenExtendProperties;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
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
        // bookmark: RouterFunction Exception Handler
        if (e instanceof HttpStatusCodeException codeException) {
            return checkLoginRedirect(request, codeException.getStatusCode(), e, ServerResponse.class);
        }
        else if (e instanceof GsvcException) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(handle(e));
        }
        else if (e instanceof WebClientResponseException responseException) {
            return ServerResponse.status(responseException.getStatusCode()).body(handle(e));
        }
        else if (e instanceof ResponseStatusException responseException) {
            return ServerResponse.status(responseException.getStatusCode()).body(handle(e));
        }

        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception error, HttpServletRequest request) {
        // bookmark: Spring Boot Global Exception Handler

        val serverRequest = ServerRequest.create(request, httpMessageConverters.getIfAvailable(Collections::emptyList));
        if (error instanceof HttpStatusCodeException codeException) {
            return checkLoginRedirect(serverRequest, codeException.getStatusCode(), error, ResponseEntity.class);
        }
        else if (error instanceof GsvcException) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(handle(error));
        }
        else if (error instanceof WebClientResponseException responseException) {
            // RPC Call
            return ResponseEntity.status(responseException.getStatusCode()).body(handle(error));
        }
        else if (error instanceof HttpRequestMethodNotSupportedException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(handle(error));
        }
        else if (error instanceof ResponseStatusException responseStatusException) {
            return ResponseEntity.status(responseStatusException.getStatusCode())
                .headers(responseStatusException.getHeaders())
                .body(handle(error));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(error));
    }

    public Response<Void> handle(Throwable e) {
        if (e instanceof GsvcException gsvcException) {
            val error = gsvcException.getError();
            return Response.error(error);
        }
        else if (e instanceof HttpRequestMethodNotSupportedException) {
            return Response.error(ServiceError.METHOD_NOT_ALLOWED);
        }
        else if (e instanceof WebClientResponseException) {
            if (e instanceof WebClientResponseException.TooManyRequests) {
                return Response.error(ServiceError.TOO_MANY_REQUESTS);
            }
            return Response.error(ServiceError.REMOTE_SERVICE_ERROR);
        }
        else if (e instanceof DegradedException) {
            return Response.error(ServiceError.DEGRADE);
        }

        return Response.error(ServiceError.SERVICE_ERROR);
    }

    @SuppressWarnings("unchecked")
    private <R> R checkLoginRedirect(ServerRequest request, HttpStatusCode status, Throwable e, Class<R> rClass) {
        // bookmark: Gateway Redirect to Login Page

        if (status == HttpStatus.UNAUTHORIZED) {
            val loginUrl = getLoginUrl(request.headers().accept());
            if (loginUrl != null) {
                if (rClass.isAssignableFrom(ServerResponse.class)) {
                    return (R) ServerResponse.status(HttpStatus.FOUND).location(loginUrl).build();
                }
                else {
                    return (R) ResponseEntity.status(HttpStatus.FOUND).location(loginUrl).build();
                }
            }
        }

        if (rClass.isAssignableFrom(ServerResponse.class)) {
            return (R) ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(e));
        }
        else {
            return (R) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(e));
        }
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