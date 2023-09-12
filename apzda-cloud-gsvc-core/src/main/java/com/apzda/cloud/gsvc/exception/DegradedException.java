package com.apzda.cloud.gsvc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author fengz
 */
public class DegradedException extends ResponseStatusException {

    public DegradedException(String reason) {
        super(HttpStatus.SERVICE_UNAVAILABLE, reason);
    }

}
