package com.apzda.cloud.gsvc.gtw;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author fengz
 */
public interface IGtwGlobalFilter<T extends ServerResponse, R extends ServerResponse>
        extends HandlerFilterFunction<T, R>, Ordered {

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
