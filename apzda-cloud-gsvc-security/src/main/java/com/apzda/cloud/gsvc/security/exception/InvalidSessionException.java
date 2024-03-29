/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.exception;

import com.apzda.cloud.gsvc.exception.NoStackLogError;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

/**
 * @author fengzi windywany@gmail.com
 **/
public class InvalidSessionException extends SessionAuthenticationException implements NoStackLogError {

    public InvalidSessionException(String msg) {
        super(msg);
    }

}
