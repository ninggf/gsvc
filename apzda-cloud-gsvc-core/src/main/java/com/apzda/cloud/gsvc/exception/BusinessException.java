package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.ServiceError;

/**
 * Business Exception.
 *
 * @author fengz windywany@gmail.com
 */
public class BusinessException extends GsvcException {

    public BusinessException(ServiceError error, Throwable e) {
        super(error, e);
    }

}
