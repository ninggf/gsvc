package com.apzda.cloud.gsvc.exception;

import com.apzda.cloud.gsvc.IServiceError;
import com.apzda.cloud.gsvc.error.InternalServiceError;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpHeaders;

/**
 * @author fengz
 */
@Getter
public class GsvcException extends RuntimeException {

    protected final IServiceError error;

    private final HttpHeaders headers;

    public GsvcException(String message) {
        this(new InternalServiceError(message), null, null);
    }

    public GsvcException(@Nonnull IServiceError error, HttpHeaders headers, Throwable e) {
        super(error.localMessage(), e);
        this.error = error;
        this.headers = headers == null ? HttpHeaders.EMPTY : headers;
    }

    public GsvcException(@Nonnull IServiceError error, HttpHeaders headers) {
        this(error, headers, null);
    }

    public GsvcException(@Nonnull IServiceError error, Throwable e) {
        this(error, null, e);
    }

    public GsvcException(@Nonnull IServiceError error) {
        this(error, null, null);
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("errCode", error.code())
            .append("errMsg", error.localMessage())
            .toString();
    }

}
