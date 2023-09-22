package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Global Exception handler.
 *
 * @author ninggf windywany@gmail.com
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GsvcExceptionHandler {

    private final ObjectProvider<List<HttpMessageConverter<?>>> httpMessageConverters;

    public ServerResponse handle(Throwable error, ServerRequest request) {
        // bookmark: RouterFunction Exception Handler
        return handle(error, request, ServerResponse.class);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception error, HttpServletRequest request) {
        // bookmark: Spring Boot Global Exception Handler

        val serverRequest = ServerRequest.create(request, httpMessageConverters.getIfAvailable(Collections::emptyList));
        return handle(error, serverRequest, ResponseEntity.class);
    }

    public Response<Void> handle(Throwable e) {
        if (e instanceof GsvcException gsvcException) {
            val error = gsvcException.getError();
            return Response.error(error);
        }
        else if (e instanceof HttpRequestMethodNotSupportedException) {
            return Response.error(ServiceError.METHOD_NOT_ALLOWED);
        }
        else if (e instanceof WebClientRequestException) {
            return Response.error(ServiceError.REMOTE_SERVICE_NO_INSTANCE);
        }
        else if (e instanceof WebClientResponseException) {
            if (e instanceof WebClientResponseException.TooManyRequests) {
                return Response.error(ServiceError.TOO_MANY_REQUESTS);
            }
            return Response.error(ServiceError.REMOTE_SERVICE_ERROR);
        }
        else if (e instanceof HttpStatusCodeException codeException) {
            val statusCode = codeException.getStatusCode();
            if (statusCode == HttpStatus.UNAUTHORIZED) {
                return Response.error(ServiceError.UNAUTHORIZED);
            }
            else if (statusCode == HttpStatus.FORBIDDEN) {
                return Response.error(ServiceError.FORBIDDEN);
            }
        }
        else if (e instanceof TimeoutException || e instanceof io.netty.handler.timeout.TimeoutException) {
            return Response.error(ServiceError.SERVICE_TIMEOUT);
        }
        else if (e instanceof DegradedException) {
            return Response.error(ServiceError.DEGRADE);
        }

        return Response.error(ServiceError.SERVICE_ERROR);
    }

    @SuppressWarnings("unchecked")
    private <R> R checkLoginRedirect(ServerRequest request, Throwable e, Class<R> rClass) {
        // bookmark: Gateway Redirect to Login Page
        HttpStatusCode status = HttpStatusCode.valueOf(500);
        if (e instanceof ErrorResponseException statusException) {
            status = statusException.getStatusCode();
        }
        else if (e instanceof HttpStatusCodeException statusCodeException) {
            status = statusCodeException.getStatusCode();
        }

        if (status == HttpStatus.UNAUTHORIZED) {
            val loginUrl = ResponseUtils.getLoginUrl(request.headers().accept());
            if (StringUtils.isNotBlank(loginUrl)) {
                if (rClass.isAssignableFrom(ServerResponse.class)) {
                    return (R) ServerResponse.status(HttpStatus.FOUND).location(URI.create(loginUrl)).build();
                }
                else {
                    return (R) ResponseEntity.status(HttpStatus.FOUND).location(URI.create(loginUrl)).build();
                }
            }
        }

        if (rClass.isAssignableFrom(ServerResponse.class)) {
            return (R) ServerResponse.status(status).body(handle(e));
        }
        else {
            return (R) ResponseEntity.status(status).body(handle(e));
        }
    }

    private <R> R handle(Throwable error, ServerRequest request, Class<R> rClazz) {
        if (error instanceof HttpStatusCodeException || error instanceof ErrorResponseException) {
            // bookmark login
            return checkLoginRedirect(request, error, rClazz);
        }
        ResponseWrapper responseWrapper;
        if (error instanceof GsvcException) {
            responseWrapper = ResponseWrapper.status(HttpStatus.SERVICE_UNAVAILABLE).body(handle(error));
        }
        else if (error instanceof TimeoutException || error instanceof io.netty.handler.timeout.TimeoutException) {
            responseWrapper = ResponseWrapper.status(HttpStatus.GATEWAY_TIMEOUT).body(handle(error));
        }
        else if (error instanceof WebClientRequestException webClientRequestException) {
            // rpc exception
            responseWrapper = ResponseWrapper.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(error));
        }
        else if (error instanceof WebClientResponseException responseException) {
            // rpc exception
            responseWrapper = ResponseWrapper.status(responseException.getStatusCode()).body(handle(error));
        }
        else if (error instanceof HttpRequestMethodNotSupportedException) {
            responseWrapper = ResponseWrapper.status(HttpStatus.METHOD_NOT_ALLOWED).body(handle(error));
        }
        else {
            responseWrapper = ResponseWrapper.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(error));
        }

        return responseWrapper.unwrap(rClazz);
    }

    static class ResponseWrapper {

        private HttpStatusCode status;

        private Response<?> body;

        private HttpHeaders headers = new HttpHeaders();

        static ResponseWrapper status(HttpStatusCode status) {
            val wrapper = new ResponseWrapper();
            wrapper.status = status;
            return wrapper;
        }

        ResponseWrapper body(Response<?> body) {
            this.body = body;
            return this;
        }

        ResponseWrapper headers(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <R> R unwrap(Class<R> rClazz) {
            if (rClazz.isAssignableFrom(ServerResponse.class)) {
                return (R) ServerResponse.status(status).body(body);
            }
            else {
                return (R) ResponseEntity.status(status).headers(headers).body(body);
            }
        }

    }

}
