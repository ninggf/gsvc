package com.apzda.cloud.gsvc.utils;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.hubspot.jackson.datatype.protobuf.ProtobufJacksonConfig;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeoutException;

/**
 * @author fengz
 */
@Slf4j
public class ResponseUtils {

    public static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        OBJECT_MAPPER.configure(JsonNodeFeature.WRITE_NULL_PROPERTIES, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void config(ProtobufJacksonConfig config) {
        OBJECT_MAPPER.registerModule(new ProtobufModule(config));
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
            return fallback(ServiceError.REMOTE_SERVICE_UNAUTHORIZED, serviceName, rClass);
        }
        else if (e instanceof WebClientResponseException.Forbidden) {
            return fallback(ServiceError.REMOTE_SERVICE_FORBIDDEN, serviceName, rClass);
        }
        else if (e instanceof WebClientResponseException.NotFound) {
            return fallback(ServiceError.REMOTE_SERVICE_NOT_FOUND, serviceName, rClass);
        }
        else if (e instanceof TimeoutException) {
            return fallback(ServiceError.REMOTE_SERVICE_TIMEOUT, serviceName, rClass);
        }
        else if (e instanceof WebClientRequestException) {
            return fallback(ServiceError.REMOTE_SERVICE_NO_INSTANCE, serviceName, rClass);
        }
        return fallback(ServiceError.REMOTE_SERVICE_ERROR, serviceName, rClass);
    }

    public static <R> R fallback(ServiceError error, String serviceName, Class<R> tClass) {
        log.error("fallback for {} - {} - {}", serviceName, tClass, error);
        if (String.class.isAssignableFrom(tClass)) {
            return tClass.cast(error.fallbackString(serviceName));
        }
        return parseResponse(error.fallbackString(serviceName), tClass);
    }

}
