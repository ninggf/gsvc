package com.apzda.cloud.gsvc.utils;

import com.apzda.cloud.gsvc.config.GlobalConfig;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.hubspot.jackson.datatype.protobuf.ProtobufJacksonConfig;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author fengz
 */
@Slf4j
public class ResponseUtils {

    public static final MediaType TEXT_MASK = MediaType.parseMediaType("text/*");

    public static final ObjectMapper OBJECT_MAPPER;

    private static GlobalConfig gsvcConfig;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        OBJECT_MAPPER.configure(JsonNodeFeature.WRITE_NULL_PROPERTIES, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void config(ProtobufJacksonConfig config, GlobalConfig globalConfig) {
        OBJECT_MAPPER.registerModule(new ProtobufModule(config));
        gsvcConfig = globalConfig;
    }

    public static void config() {
        OBJECT_MAPPER.registerModule(new ProtobufModule());
    }

    public static <T> T parseResponse(String responseBody, Class<T> tClass) {
        // bookmark 解析响应
        try {
            return OBJECT_MAPPER.readValue(responseBody, tClass);
        }
        catch (JsonProcessingException e) {
            val requestId = GsvcContextHolder.getRequestId();
            log.error("[{}] Cannot convert 【{}】 to class: {}", requestId, responseBody, tClass);
            try {
                // bookmark: fallback to jackson error
                return OBJECT_MAPPER.readValue(ServiceError.JACKSON_ERROR.fallbackString, tClass);
            }
            catch (JsonProcessingException ex) {
                // cannot get here!!!
                throw new RuntimeException(ex);
            }
        }
    }

    public static <R> R fallback(Throwable e, String serviceName, Class<R> rClass) {
        if (e instanceof WebClientResponseException.Unauthorized) {
            return fallback(ServiceError.UNAUTHORIZED, serviceName, rClass);
        }
        else if (e instanceof WebClientResponseException.Forbidden) {
            return fallback(ServiceError.FORBIDDEN, serviceName, rClass);
        }
        else if (e instanceof WebClientResponseException.NotFound) {
            return fallback(ServiceError.NOT_FOUND, serviceName, rClass);
        }
        else if (e instanceof WebClientResponseException.TooManyRequests) {
            return fallback(ServiceError.TOO_MANY_REQUESTS, serviceName, rClass);
        }
        else if (e instanceof WebClientResponseException.ServiceUnavailable || e instanceof WebClientRequestException) {
            return fallback(ServiceError.REMOTE_SERVICE_NO_INSTANCE, serviceName, rClass);
        }
        else if (e instanceof WebClientResponseException.GatewayTimeout || e instanceof TimeoutException) {
            return fallback(ServiceError.SERVICE_TIMEOUT, serviceName, rClass);
        }

        return fallback(ServiceError.REMOTE_SERVICE_ERROR, serviceName, rClass);
    }

    public static <R> R fallback(ServiceError error, String serviceName, Class<R> tClass) {
        if (String.class.isAssignableFrom(tClass)) {
            return tClass.cast(error.fallbackString(serviceName));
        }
        return parseResponse(error.fallbackString(serviceName), tClass);
    }

    public static void respond(HttpServletRequest request, HttpServletResponse response, Response<?> data)
            throws IOException {
        val errCode = data.getErrCode();
        val serverHttpRequest = new ServletServerHttpRequest(request);
        val mediaTypes = serverHttpRequest.getHeaders().getAccept();

        if (isCompatibleWith(TEXT_MASK, mediaTypes)) {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            if (errCode == -401) {
                val loginUrl = getLoginUrl(mediaTypes);
                if (loginUrl != null) {
                    response.sendRedirect(loginUrl.toString());
                    return;
                }
            }
        }
        else if (isCompatibleWith(MediaType.APPLICATION_JSON, mediaTypes)) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        if (ServiceError.isHttpError(errCode)) {
            response.setStatus(Math.abs(errCode));
        }
        else if (errCode != 0) {
            response.setStatus(500);
        }

        val jsonStr = OBJECT_MAPPER.writeValueAsString(data);
        response.setContentLength(jsonStr.length());

        try (val writer = response.getWriter()) {
            writer.write(jsonStr);
        }
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

    public static List<MediaType> mediaTypes(HttpServletRequest request) {
        val serverHttpRequest = new ServletServerHttpRequest(request);
        return serverHttpRequest.getHeaders().getAccept();
    }

    public static URI getLoginUrl(List<MediaType> contentTypes) {
        if (gsvcConfig != null) {
            val loginUrl = gsvcConfig.getLoginPage();

            if (loginUrl != null && ResponseUtils.isCompatibleWith(TEXT_MASK, contentTypes)) {
                return loginUrl;
            }
        }
        return null;
    }

}
