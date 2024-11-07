package com.apzda.cloud.gsvc;

import com.apzda.cloud.gsvc.dto.MessageType;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

/**
 * @author fengz
 */
public interface IServiceError {

    Object[] emptyArgs = new Object[] {};

    int code();

    default int httpCode() {
        return 0;
    }

    default String message() {
        return "";
    }

    default String localMessage() {
        val message = message();
        val code = code();
        if (code != 0 && StringUtils.isBlank(message)) {
            return I18nUtils.t("error." + Math.abs(code), args());
        }
        else if (StringUtils.startsWith(message, "{") && StringUtils.endsWith(message, "}")) {
            val msg = message.substring(1, message.length() - 1);
            return I18nUtils.t(msg, args(), msg);
        }

        return message;
    }

    default Object[] args() {
        return emptyArgs;
    }

    default MessageType type() {
        return null;
    }

    default void emit() {
        throw new GsvcException(this);
    }

    default void emit(Throwable e) {
        throw new GsvcException(this, null, e);
    }

    default void emit(HttpHeaders headers, Throwable e) {
        throw new GsvcException(this, headers, e);
    }

}
