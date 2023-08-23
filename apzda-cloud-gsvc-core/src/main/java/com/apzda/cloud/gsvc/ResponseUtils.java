package com.apzda.cloud.gsvc;

import com.apzda.cloud.gsvc.core.ServiceConfigurationProperties;
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

    public static void config(ServiceConfigurationProperties properties) {
        val config = properties.getConfig();
        val pbConfig = ProtobufJacksonConfig.builder().acceptLiteralFieldnames(config.isAcceptLiteralFieldNames())
                                            .properUnsignedNumberSerialization(config.isProperUnsignedNumberSerialization())
                                            .serializeLongsAsString(config.isSerializeLongsAsString()).build();
        OBJECT_MAPPER.registerModule(new ProtobufModule(pbConfig));
    }

    public static void config() {
        OBJECT_MAPPER.registerModule(new ProtobufModule());
    }

    public static <T> T parseResponse(String responseBody, Class<T> tClass) {
        // bookmark 解析响应
        try {
            return OBJECT_MAPPER.readValue(responseBody, tClass);
        } catch (JsonProcessingException e) {
            log.error("Cannot convert 【{}】 to class: {}", StringUtils.truncate(responseBody, 256), tClass);
            try {
                //bookmark: fallback to jackson error
                return OBJECT_MAPPER.readValue(ServiceError.JACKSON_ERROR.fallbackString, tClass);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static <T> T fallback(ServiceError error, String serviceName, Class<T> tClass) {
        return parseResponse(error.fallbackString(serviceName), tClass);
    }
}
