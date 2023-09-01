package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.IServiceError;
import lombok.Getter;
import org.springframework.core.style.ToStringCreator;

/**
 * @author fengz
 */
@Getter
public class GsvcException extends RuntimeException {

    private final IServiceError error;

    public GsvcException(IServiceError error, Throwable e) {
        super(error.message(), e);
        this.error = error;
    }

    public GsvcException(IServiceError error) {
        super(error.message());
        this.error = error;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("errCode", error.code()).append("errMsg", error.message()).toString();
    }

}
