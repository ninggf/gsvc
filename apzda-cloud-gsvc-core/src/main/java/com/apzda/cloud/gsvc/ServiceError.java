package com.apzda.cloud.gsvc;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

/**
 * @author ninggf
 */

public enum ServiceError {
    REMOTE_SERVICE_UNAUTHORIZED(-401, HttpStatus.UNAUTHORIZED.getReasonPhrase()),
    REMOTE_SERVICE_FORBIDDEN(-403, HttpStatus.FORBIDDEN.getReasonPhrase()),
    REMOTE_SERVICE_NOT_FOUND(-404, HttpStatus.NOT_FOUND.getReasonPhrase()),
    SERVICE_ERROR(-500, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()),
    REMOTE_SERVICE_ERROR(-501, "Remote Service error"),
    REMOTE_SERVICE_NO_INSTANCE(-502, "No Service instance(server) found"),
    REMOTE_SERVICE_TIMEOUT(-504, "RPC timeout"),
    JACKSON_ERROR(-999, "Invalid JSON data");
    @JsonValue
    public final int code;
    public final String message;

    public final String fallbackString;

    ServiceError(int code, String message) {
        this.code = code;
        this.message = message;
        this.fallbackString = """
            {"errCode":%d,"errMsg":"[fallback] %s"}
            """.formatted(code, message);
    }

    public String fallbackString(String service) {
        return """
            {"errCode":%d,"errMsg":"[fallback] %s [%s]"}
            """.formatted(code, message, service);
    }

    public static boolean isInternalError(ServiceError error) {
        return error.code >= -999 && error.code <= -400;
    }

    public static String cleanMessage(ServiceError error) {
        if (StringUtils.isNotBlank(error.message) && StringUtils.startsWith(error.message, "[fallback] ")) {
            return error.message.substring(11);
        }
        return error.message;
    }
}
