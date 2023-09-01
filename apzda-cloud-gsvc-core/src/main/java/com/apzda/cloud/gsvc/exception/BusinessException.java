package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.IServiceError;

/**
 * Business Exception.
 *
 * @author fengz windywany@gmail.com
 */
public class BusinessException extends GsvcException {

    public BusinessException(IServiceError error, Throwable e) {
        super(error, e);
    }

    public BusinessException(IServiceError error) {
        super(error);
    }

}
