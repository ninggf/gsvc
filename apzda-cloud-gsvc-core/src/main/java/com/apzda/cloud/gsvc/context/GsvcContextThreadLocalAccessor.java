package com.apzda.cloud.gsvc.context;

import io.micrometer.context.ThreadLocalAccessor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author fengz
 */
public class GsvcContextThreadLocalAccessor implements ThreadLocalAccessor<RequestAttributes> {

    public static final String KEY = "gsvc.context";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public RequestAttributes getValue() {
        return RequestContextHolder.getRequestAttributes();
    }

    @Override
    public void setValue(RequestAttributes value) {
        RequestContextHolder.setRequestAttributes(value);
    }

    @Override
    public void setValue() {
        RequestContextHolder.setRequestAttributes(null);
    }

}
