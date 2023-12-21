package com.apzda.cloud.gsvc.exception;

import build.buf.validate.Violation;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.utils.I18nHelper;
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
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
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

    private final ObjectProvider<List<ExceptionTransformer>> transformers;

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

    public Response<?> handle(Throwable e) {
        e = transform(e);
        if (e instanceof GsvcException gsvcException) {
            val error = gsvcException.getError();
            return Response.error(error);
        }
        else if (e instanceof MessageValidationException validationException) {
            val violations = new HashMap<String, String>();
            val fullName = validationException.getDescriptor().getFullName();
            for (Violation violation : validationException.getViolations()) {
                val field = violation.getFieldPath();
                if (log.isDebugEnabled()) {
                    log.debug("Add code: '{}' to message resource property file to support i18n",
                            fullName + "." + field);
                }
                val message = I18nHelper.t(fullName + "." + field, violation.getMessage());
                violations.put(field, message);
            }

            return Response.error(ServiceError.BIND_ERROR, violations);
        }
        else if (e instanceof BindException bindException) {
            val violations = new HashMap<String, String>();
            for (FieldError error : bindException.getFieldErrors()) {
                violations.put(error.getField(), I18nHelper.t(error));
            }
            return Response.error(ServiceError.BIND_ERROR, violations);
        }
        else if (e instanceof HttpMessageConversionException readableException) {
            return Response.error(ServiceError.INVALID_FORMAT, readableException.getMessage());
        }
        else if (e instanceof MethodArgumentTypeMismatchException typeMismatchException) {
            val violations = new HashMap<String, String>();
            violations.put(typeMismatchException.getName(), e.getMessage());
            return Response.error(ServiceError.BIND_ERROR, violations);
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
            return handleHttpStatusError(codeException.getStatusCode(), codeException.getMessage());
        }
        else if (e instanceof TimeoutException || e instanceof io.netty.handler.timeout.TimeoutException) {
            return Response.error(ServiceError.SERVICE_TIMEOUT);
        }
        else if (e instanceof DegradedException) {
            return Response.error(ServiceError.DEGRADE);
        }
        else if (e instanceof ErrorResponseException codeException) {
            return handleHttpStatusError(codeException.getStatusCode(), codeException.getMessage());
        }
        else if (e instanceof ErrorResponse errorResponse) {
            return Response.error(errorResponse.getBody().getStatus(), errorResponse.getBody().getDetail());
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

        return null;
    }

    private <R> R handle(Throwable error, ServerRequest request, Class<R> rClazz) {
        error = transform(error);
        if (error instanceof HttpStatusCodeException || error instanceof ErrorResponseException) {
            // bookmark login
            val handled = checkLoginRedirect(request, error, rClazz);
            if (handled != null) {
                return handled;
            }
        }
        ResponseWrapper responseWrapper;
        if (error instanceof GsvcException gsvcException) {
            responseWrapper = ResponseWrapper.status(HttpStatus.SERVICE_UNAVAILABLE).body(handle(error));
            responseWrapper.headers(gsvcException.getHeaders());
        }
        else if (error instanceof TimeoutException || error instanceof io.netty.handler.timeout.TimeoutException) {
            responseWrapper = ResponseWrapper.status(HttpStatus.GATEWAY_TIMEOUT).body(handle(error));
        }
        else if (error instanceof WebClientRequestException) {
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
        else if (error instanceof ErrorResponseException responseException) {
            responseWrapper = ResponseWrapper.status(responseException.getStatusCode()).body(handle(error));
            responseWrapper.headers(responseException.getHeaders());
        }
        else if (error instanceof HttpStatusCodeException httpStatusCodeException) {
            responseWrapper = ResponseWrapper.status(httpStatusCodeException.getStatusCode()).body(handle(error));
            responseWrapper.headers(httpStatusCodeException.getResponseHeaders());
        }
        else if (error instanceof MessageValidationException || error instanceof BindException
                || error instanceof HttpMessageConversionException
                || error instanceof MethodArgumentTypeMismatchException) {
            responseWrapper = ResponseWrapper.ok().body(handle(error));
            log.warn("[{}] Exception Resolved[{}: {}]", GsvcContextHolder.getRequestId(), error.getClass().getName(),
                    error.getMessage());
            return responseWrapper.unwrap(rClazz);
        }
        else if (error instanceof ErrorResponse errorResponse) {
            responseWrapper = ResponseWrapper.status(errorResponse.getBody().getStatus()).body(handle(error));
            return responseWrapper.unwrap(rClazz);
        }
        else {
            responseWrapper = ResponseWrapper.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(error));
        }
        if (!(error instanceof NoStackLogError)) {
            log.error("[{}] Exception Resolved:", GsvcContextHolder.getRequestId(), error);
        }
        return responseWrapper.unwrap(rClazz);
    }

    private Throwable transform(Throwable throwable) {
        val ts = transformers.getIfAvailable();
        if (ts == null) {
            return throwable;
        }
        val aClass = throwable.getClass();
        for (ExceptionTransformer t : ts) {
            if (t.supports(aClass)) {
                val te = t.transform(throwable);
                if (te != null) {
                    return te;
                }
            }
        }
        return throwable;
    }

    private Response<?> handleHttpStatusError(HttpStatusCode statusCode, String message) {
        if (statusCode == HttpStatus.UNAUTHORIZED) {
            return Response.error(ServiceError.UNAUTHORIZED);
        }
        else if (statusCode == HttpStatus.FORBIDDEN) {
            return Response.error(ServiceError.FORBIDDEN);
        }
        return Response.error(statusCode.value(), message);
    }

    final static class ResponseWrapper {

        private HttpStatusCode status;

        private Response<?> body;

        private HttpHeaders headers = new HttpHeaders();

        static ResponseWrapper status(HttpStatusCode status) {
            val wrapper = new ResponseWrapper();
            wrapper.status = status;
            return wrapper;
        }

        static ResponseWrapper status(int status) {
            val wrapper = new ResponseWrapper();
            wrapper.status = HttpStatusCode.valueOf(status);
            return wrapper;
        }

        static ResponseWrapper ok() {
            val wrapper = new ResponseWrapper();
            wrapper.status = HttpStatus.OK;
            return wrapper;
        }

        ResponseWrapper body(Response<?> body) {
            this.body = body;
            return this;
        }

        void headers(HttpHeaders headers) {
            this.headers = headers;
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
