package com.apzda.cloud.gsvc.context;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import io.micrometer.context.ThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author fengz
 */
@Slf4j
public class GsvcContextThreadLocalAccessor implements ThreadLocalAccessor<GsvcContextHolder.GsvcContext> {

    public static final String KEY = "gsvc.context";

    @Override
    @NonNull
    public Object key() {
        return KEY;
    }

    @Override
    public GsvcContextHolder.GsvcContext getValue() {
        val context = GsvcContextHolder.CONTEXT_BOX.get();
        if (context != null && context.getAttributes() == null) {
            context.setAttributes(RequestContextHolder.getRequestAttributes());
        }

        return context;
    }

    @Override
    public void setValue(@NonNull GsvcContextHolder.GsvcContext value) {
        GsvcContextHolder.CONTEXT_BOX.set(value);
        if (value.getAttributes() != null) {
            RequestContextHolder.setRequestAttributes(value.getAttributes());
        }
    }

    @Override
    public void setValue() {
        GsvcContextHolder.CONTEXT_BOX.remove();
        RequestContextHolder.setRequestAttributes(null);
    }

}
