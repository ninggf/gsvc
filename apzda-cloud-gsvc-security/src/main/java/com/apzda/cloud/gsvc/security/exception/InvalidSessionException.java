/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * @author fengzi windywany@gmail.com
 **/
public class InvalidSessionException extends AuthenticationException {

    public InvalidSessionException(String msg) {
        super(msg);
    }

}
