package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.ServiceError;
import lombok.Getter;
import org.springframework.core.style.ToStringCreator;

/**
 * @author fengz
 */
@Getter
public class GsvcException extends RuntimeException {
    private final ServiceError errorCode;

    public GsvcException(ServiceError errorCode) {
        super(errorCode.message);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this)
            .append("errCode", errorCode.code)
            .append("errMsg", errorCode.message)
            .toString();
    }
}
