package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.IServiceError;
import org.springframework.http.HttpHeaders;

/**
 * Business Exception.
 *
 * @author fengz windywany@gmail.com
 */
public class BusinessException extends GsvcException {

    public BusinessException(IServiceError error, HttpHeaders headers, Throwable e) {
        super(error, headers, e);
    }

    public BusinessException(IServiceError error, Throwable e) {
        this(error, null, e);
    }

    public BusinessException(IServiceError error) {
        this(error, null);
    }

}
