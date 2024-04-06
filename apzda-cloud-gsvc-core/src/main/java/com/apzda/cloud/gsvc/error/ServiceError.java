package com.apzda.cloud.gsvc.error;

import com.apzda.cloud.gsvc.IServiceError;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

/**
 * @author ninggf
 */

public enum ServiceError implements IServiceError {

    //@formatter:off
    BAD_REQUEST(-400, HttpStatus.BAD_REQUEST.getReasonPhrase()),
    UNAUTHORIZED(-401, HttpStatus.UNAUTHORIZED.getReasonPhrase()),
    FORBIDDEN(-403, HttpStatus.FORBIDDEN.getReasonPhrase()),
    NOT_FOUND(-404, HttpStatus.NOT_FOUND.getReasonPhrase()),
    METHOD_NOT_ALLOWED(-405, HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase()),
    TOO_MANY_REQUESTS(-429, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase()),
    SERVICE_ERROR(-500, "Service Internal Error"),
    REMOTE_SERVICE_ERROR(-501, "Service RPC Error"),
    REMOTE_SERVICE_NO_INSTANCE(-502, "No Service instance found"),
    SERVICE_UNAVAILABLE(-503, HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()),
    SERVICE_TIMEOUT(-504, "Service RPC Timeout"),
    INVALID_PRINCIPAL_TYPE(-800, "Unknown Principal type"),
    MFA_NOT_SETUP(-801,"Mfa not setup"),
    MFA_NOT_VERIFIED(-802,"Mfa not verified"),
    TOKEN_EXPIRED(-810,"Access Token expired"),
    TOKEN_INVALID(-811,"Token is invalid"),
    USER_PWD_INCORRECT(-812,"Username or Password is incorrect"),
    CREDENTIALS_EXPIRED(-813,"Credentials is expired"),
    ACCOUNT_EXPIRED(-814,"Account is expired"),
    ACCOUNT_LOCKED(-815,"Account is locked"),
    ACCOUNT_UN_AUTHENTICATED(-816,"Account is unAuthenticated"),
    ACCOUNT_DISABLED(-817,"Account is disabled"),
    DEVICE_NOT_ALLOWED(-818,"Device is not allowed"),
    INVALID_FORMAT(-996, "Invalid Format"),
    BIND_ERROR(-997, "Data Is Invalid"),
    DEGRADE(-998, "Service has been Degraded"),
    JACKSON_ERROR(-999, "Invalid JSON data");
    //@formatter:on
    @JsonValue
    public final int code;

    public final String message;

    public final String fallbackString;

    ServiceError(int code, String message) {
        this.code = code;
        this.message = message;
        this.fallbackString = """
                {"errCode":%d,"errMsg":"%s"}
                """.formatted(code, message());
    }

    public String fallbackString(String service) {
        return """
                {"errCode":%d,"errMsg":"%s"}
                """.formatted(code, message.replace("Service ", "Service(" + service + ") "));
    }

    public String fallbackString(String service, String error) {
        val detail = StringUtils.defaultIfBlank(error, "").replace("\"", "\\\"");
        return """
                {"errCode":%d,"errMsg":"%s(%s)"}
                """.formatted(code, message.replace("Service ", "Service(" + service + ") "), detail);
    }

    public static boolean isInternalError(ServiceError error) {
        return error.code >= -999 && error.code <= -400;
    }

    public static boolean isHttpError(ServiceError error) {
        return isHttpError(error.code);
    }

    public static boolean isHttpError(int errCode) {
        return HttpStatus.resolve(Math.abs(errCode)) != null;
    }

    public static ServiceError valueOf(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ServiceError.BAD_REQUEST;
            case UNAUTHORIZED -> ServiceError.UNAUTHORIZED;
            case FORBIDDEN -> ServiceError.FORBIDDEN;
            case NOT_FOUND -> ServiceError.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ServiceError.METHOD_NOT_ALLOWED;
            case TOO_MANY_REQUESTS -> ServiceError.DEGRADE;
            case BAD_GATEWAY -> ServiceError.REMOTE_SERVICE_NO_INSTANCE;
            case SERVICE_UNAVAILABLE -> ServiceError.SERVICE_UNAVAILABLE;
            case GATEWAY_TIMEOUT -> ServiceError.SERVICE_TIMEOUT;
            default -> ServiceError.SERVICE_ERROR;
        };
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        val context = GsvcContextHolder.current();
        val svcName = context.getSvcName();
        if (svcName != null) {
            return message.replace("Service ", "Service(" + svcName + ") ");
        }

        return message;
    }

}
