package com.apzda.cloud.gsvc.context;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import io.micrometer.context.ThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author fengz
 */
@Slf4j
public class GsvcContextThreadLocalAccessor implements ThreadLocalAccessor<RequestAttributes> {

    public static final String KEY = "gsvc.context";

    @Override
    @NonNull
    public Object key() {
        return KEY;
    }

    @Override
    public RequestAttributes getValue() {
        return RequestContextHolder.getRequestAttributes();
    }

    @Override
    public void setValue(@NonNull RequestAttributes value) {
        RequestContextHolder.setRequestAttributes(value);
        val requestId = GsvcContextHolder.getRequestId();
        if (StringUtils.isNotBlank(requestId)) {
            GsvcContextHolder.setRequestId(requestId);
        }
    }

    @Override
    public void setValue() {
        RequestContextHolder.setRequestAttributes(null);
        GsvcContextHolder.setRequestId(null);
    }

}
