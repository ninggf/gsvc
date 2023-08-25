package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.ServiceError;
import lombok.Getter;
import org.springframework.core.style.ToStringCreator;

/**
 * @author fengz
 */
@Getter
public class GsvcException extends RuntimeException {
    private final ServiceError error;

    public GsvcException(ServiceError error, Throwable e) {
        super(error.message, e);
        this.error = error;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this)
            .append("errCode", error.code)
            .append("errMsg", error.message)
            .toString();
    }
}
