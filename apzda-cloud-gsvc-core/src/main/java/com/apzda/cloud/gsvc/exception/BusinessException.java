package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.IServiceError;
import jakarta.annotation.Nonnull;
import org.springframework.http.HttpHeaders;

/**
 * Business Exception.
 *
 * @author fengz windywany@gmail.com
 */
public class BusinessException extends GsvcException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(@Nonnull IServiceError error, HttpHeaders headers, Throwable e) {
        super(error, headers, e);
    }

    public BusinessException(@Nonnull IServiceError error, Throwable e) {
        this(error, null, e);
    }

    public BusinessException(@Nonnull IServiceError error) {
        this(error, null);
    }

}
