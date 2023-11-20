package com.apzda.cloud.gsvc;

import com.apzda.cloud.gsvc.dto.MessageType;
import com.apzda.cloud.gsvc.exception.GsvcException;
import org.springframework.http.HttpHeaders;

/**
 * @author fengz
 */
public interface IServiceError {

    int code();

    String message();

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
