package com.apzda.cloud.gsvc.exception;

import org.springframework.web.ErrorResponseException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface ExceptionTransformer {

    ErrorResponseException transform(Throwable exception);

    boolean supports(Class<? extends Throwable> eClass);

}
