package com.apzda.cloud.gsvc.utils;

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
import org.springframework.util.StringUtils;

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
            log.error("Cannot convert 【{}】 to class: {}", StringUtils.truncate(responseBody, 256), tClass);
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

    public static <R> R fallback(ServiceError error, String serviceName, Class<R> tClass) {
        if (String.class.isAssignableFrom(tClass)) {
            return tClass.cast(error.fallbackString(serviceName));
        }
        return parseResponse(error.fallbackString(serviceName), tClass);
    }

}
