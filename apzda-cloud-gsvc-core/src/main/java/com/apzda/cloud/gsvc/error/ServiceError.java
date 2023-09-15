package com.apzda.cloud.gsvc.error;

import com.apzda.cloud.gsvc.IServiceError;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;

/**
 * @author ninggf
 */

public enum ServiceError implements IServiceError {

    BAD_REQUEST(-400, HttpStatus.BAD_REQUEST.getReasonPhrase()),
    UNAUTHORIZED(-401, HttpStatus.UNAUTHORIZED.getReasonPhrase()),
    FORBIDDEN(-403, HttpStatus.FORBIDDEN.getReasonPhrase()), NOT_FOUND(-404, HttpStatus.NOT_FOUND.getReasonPhrase()),
    METHOD_NOT_ALLOWED(-405, HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase()),
    TOO_MANY_REQUESTS(-429, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase()),
    SERVICE_ERROR(-500, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()),
    REMOTE_SERVICE_ERROR(-501, "Remote Service error"),
    REMOTE_SERVICE_NO_INSTANCE(-502, "No Service instance(server) found"),
    SERVICE_UNAVAILABLE(-503, HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()),
    SERVICE_TIMEOUT(-504, "Service Execution Timeout"), INVALID_PRINCIPAL_TYPE(-800, "Unknown Principal type"),
    DEGRADE(-998, "Service Degrade"), JACKSON_ERROR(-999, "Invalid JSON data");

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
                {"errCode":%d,"errMsg":"%s [%s]"}
                """.formatted(code, message, service);
    }

    public static boolean isInternalError(ServiceError error) {
        return error.code >= -999 && error.code <= -400;
    }

    public static boolean isHttpError(ServiceError error) {
        return isHttpError(error.code);
    }

    public static boolean isHttpError(int errCode) {
        return errCode >= -599 && errCode <= -400;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

}
