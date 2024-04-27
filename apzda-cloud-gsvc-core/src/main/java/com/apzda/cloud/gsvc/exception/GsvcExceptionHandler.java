package com.apzda.cloud.gsvc.exception;

import build.buf.validate.Violation;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.gtw.filter.HttpHeadersFilter;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.SocketException;
import java.net.URI;
import java.rmi.UnknownHostException;
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
public class GsvcExceptionHandler implements IExceptionHandler, ApplicationContextAware {

    private final ObjectProvider<List<HttpMessageConverter<?>>> httpMessageConverters;

    private final ObjectProvider<List<ExceptionTransformer>> transformers;

    private HttpHeadersFilter removeHopByHopHeadersFilter;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.removeHopByHopHeadersFilter = applicationContext.getBean("removeHopByHopHeadersFilter",
                HttpHeadersFilter.class);
    }

    @Override
    public ServerResponse handle(Throwable error, ServerRequest request) {
        // bookmark: RouterFunction Exception Handler
        return handle(error, request, ServerResponse.class);
    }

    public ServerResponse handle(Throwable error, HttpServletRequest request) {
        // bookmark: ProxyExchange Exception Handler

        val serverRequest = ServerRequest.create(request, httpMessageConverters.getIfAvailable(Collections::emptyList));
        return handle(error, serverRequest, ServerResponse.class);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception error, HttpServletRequest request) {
        // bookmark: Spring Boot Global Exception Handler

        val serverRequest = ServerRequest.create(request, httpMessageConverters.getIfAvailable(Collections::emptyList));
        return handle(error, serverRequest, ResponseEntity.class);
    }

    public Response<?> handle(Throwable throwable, boolean transform) {
        val e = transform ? transform(throwable) : throwable;
        if (e instanceof GsvcException gsvcException) {
            val error = gsvcException.getError();
            return Response.error(error);
        }
        else if (e instanceof MessageValidationException validationException) {
            val violations = new HashMap<String, String>();
            val fullName = validationException.getDescriptor().getFullName();
            for (Violation violation : validationException.getViolations()) {
                val field = violation.getFieldPath();
                val i8nKey = "valid." + fullName + "." + field;
                if (log.isDebugEnabled()) {
                    log.debug("Add code: '{}' to message resource property file to support i18n", i8nKey);
                }
                val message = I18nUtils.t(i8nKey, violation.getMessage());
                violations.put(field, message);
            }

            return Response.error(ServiceError.BIND_ERROR, violations);
        }
        else if (e instanceof BindException bindException) {
            val violations = new HashMap<String, String>();
            for (FieldError error : bindException.getFieldErrors()) {
                violations.put(error.getField(), I18nUtils.t(error));
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
        else if (e instanceof TimeoutException || e instanceof io.netty.handler.timeout.TimeoutException) {
            return Response.error(ServiceError.SERVICE_TIMEOUT);
        }
        else if (e instanceof WebClientRequestException || e instanceof UnknownHostException
                || e instanceof SocketException) {
            return Response.error(ServiceError.REMOTE_SERVICE_NO_INSTANCE);
        }
        else if (e instanceof WebClientResponseException responseException) {
            return Response.error(responseException.getStatusCode().value(), responseException.getStatusText());
        }
        else if (e instanceof HttpStatusCodeException codeException) {
            return handleHttpStatusError(codeException.getStatusCode(), codeException.getMessage());
        }
        else if (e instanceof DegradedException) {
            return Response.error(ServiceError.DEGRADE);
        }
        else if (e instanceof ErrorResponseException codeException) {
            return handleHttpStatusError(codeException.getStatusCode(), codeException.getBody().getDetail());
        }
        else if (e instanceof ErrorResponse errorResponse) {
            val body = errorResponse.getBody();
            return Response.error(body.getStatus(), body.getDetail());
        }

        return Response.error(ServiceError.SERVICE_ERROR);
    }

    public Response<?> handle(Throwable e) {
        return handle(e, false);
    }

    @SuppressWarnings("unchecked")
    private <R> R checkLoginRedirect(ServerRequest request, Throwable e, Class<R> rClass) {
        // bookmark: Gateway Redirect to Login Page
        HttpStatusCode status = HttpStatusCode.valueOf(500);
        HttpHeaders headers = new HttpHeaders();
        if (e instanceof ErrorResponseException statusException) {
            status = statusException.getStatusCode();
            headers = new HttpHeaders(statusException.getHeaders());
        }
        else if (e instanceof HttpStatusCodeException statusCodeException) {
            status = statusCodeException.getStatusCode();
            if (statusCodeException.getResponseHeaders() != null) {
                headers = new HttpHeaders(statusCodeException.getResponseHeaders());
            }
        }
        else if (e instanceof WebClientResponseException responseException) {
            status = responseException.getStatusCode();
            headers = new HttpHeaders(responseException.getHeaders());
        }

        val mediaTypes = request.headers().accept();
        var mediaType = mediaTypes.isEmpty() ? MediaType.APPLICATION_JSON : mediaTypes.get(0).removeQualityValue();
        if (mediaType.isWildcardType()) {
            mediaType = MediaType.APPLICATION_JSON;
        }
        else if (mediaType.isWildcardSubtype()) {
            mediaType = switch (mediaType.getType()) {
                case "*", "text" -> MediaType.TEXT_PLAIN;
                case "application" -> MediaType.APPLICATION_JSON;

                default -> throw new IllegalStateException("Unexpected value: " + mediaType.getType());
            };
        }

        if (status.is2xxSuccessful() || status.is3xxRedirection()) {
            val filtered = removeHopByHopHeadersFilter.filter(headers, null);

            if (rClass.isAssignableFrom(ServerResponse.class)) {
                return (R) ServerResponse.status(status)
                    .contentType(mediaType)
                    .headers(rHeaders -> rHeaders.putAll(filtered))
                    .build();
            }
            else {
                return (R) ResponseEntity.status(status).contentType(mediaType).headers(filtered).build();
            }
        }
        else if (status == HttpStatus.UNAUTHORIZED) {
            val loginUrl = ResponseUtils.getLoginUrl(mediaTypes);
            if (StringUtils.isNotBlank(loginUrl)) {
                if (rClass.isAssignableFrom(ServerResponse.class)) {
                    return (R) ServerResponse.status(HttpStatus.TEMPORARY_REDIRECT)
                        .contentType(mediaType)
                        .location(URI.create(loginUrl))
                        .build();
                }
                else {
                    return (R) ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                        .location(URI.create(loginUrl))
                        .contentType(mediaType)
                        .build();
                }
            }
        }

        return null;
    }

    private <R> R handle(Throwable error, ServerRequest request, Class<R> rClazz) {
        error = transform(error);
        if (error instanceof HttpStatusCodeException || error instanceof ErrorResponse
                || error instanceof WebClientResponseException) {
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

            return responseWrapper.unwrap(rClazz, error);
        }
        else if (error instanceof WebClientRequestException || error instanceof UnknownHostException
                || error instanceof SocketException) {
            // rpc exception
            responseWrapper = ResponseWrapper.status(HttpStatus.BAD_GATEWAY).body(handle(error));

            return responseWrapper.unwrap(rClazz, error);
        }
        else if (error instanceof WebClientResponseException responseException) {
            // rpc exception
            responseWrapper = ResponseWrapper.status(responseException.getStatusCode()).body(handle(error));

            return responseWrapper.unwrap(rClazz, error);
        }
        else if (error instanceof ErrorResponseException responseException) {
            responseWrapper = ResponseWrapper.status(responseException.getStatusCode()).body(handle(error));
            responseWrapper.headers(responseException.getHeaders());

            return responseWrapper.unwrap(rClazz, error);
        }
        else if (error instanceof HttpStatusCodeException httpStatusCodeException) {
            responseWrapper = ResponseWrapper.status(httpStatusCodeException.getStatusCode()).body(handle(error));
            responseWrapper.headers(httpStatusCodeException.getResponseHeaders());

            return responseWrapper.unwrap(rClazz, error);
        }
        else if (error instanceof MessageValidationException || error instanceof BindException
                || error instanceof HttpMessageConversionException
                || error instanceof MethodArgumentTypeMismatchException) {
            responseWrapper = ResponseWrapper.status(HttpStatus.BAD_REQUEST).body(handle(error));

            return responseWrapper.unwrap(rClazz, error);
        }
        else if (error instanceof ErrorResponse errorResponse) {
            responseWrapper = ResponseWrapper.status(errorResponse.getBody().getStatus()).body(handle(error));
            responseWrapper.headers(errorResponse.getHeaders());

            return responseWrapper.unwrap(rClazz, error);
        }
        else {
            responseWrapper = ResponseWrapper.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(error));
        }
        if (!(error instanceof NoStackLogError)) {
            log.error("Exception Resolved:", error);
        }
        return responseWrapper.unwrap(rClazz, null);
    }

    private Throwable transform(Throwable throwable) {
        while (throwable.getClass().equals(RuntimeException.class) && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

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
        else if (statusCode == HttpStatus.BAD_GATEWAY) {
            return Response.error(ServiceError.REMOTE_SERVICE_NO_INSTANCE);
        }
        else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
            return Response.error(ServiceError.SERVICE_UNAVAILABLE);
        }
        else if (statusCode == HttpStatus.GATEWAY_TIMEOUT) {
            return Response.error(ServiceError.SERVICE_TIMEOUT);
        }
        else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
            return Response.error(-500, ServiceError.SERVICE_ERROR.message() + " - " + message);
        }

        return Response.error(statusCode.value(), message);
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

        static ResponseWrapper status(int status) {
            val wrapper = new ResponseWrapper();
            wrapper.status = HttpStatusCode.valueOf(status);
            return wrapper;
        }

        ResponseWrapper body(Response<?> body) {
            this.body = body;
            return this;
        }

        void headers(HttpHeaders headers) {
            if (headers != null) {
                this.headers = headers;
            }
        }

        @SuppressWarnings("unchecked")
        public <R> R unwrap(Class<R> rClazz, @Nullable Throwable error) {
            if (error != null) {
                log.error("Exception Resolved[{}]: {}", error.getClass().getName(), error.getMessage());
            }

            if (rClazz.isAssignableFrom(ServerResponse.class)) {
                return (R) ServerResponse.status(status).headers(httpHeaders -> {
                    httpHeaders.putAll(this.headers);
                }).body(body);
            }
            else {
                return (R) ResponseEntity.status(status).headers(headers).body(body);
            }
        }

    }

}
