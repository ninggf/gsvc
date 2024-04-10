package com.apzda.cloud.demo.foo.filter;

import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author fengz
 */
@Component
@Slf4j
public class FooDemoFilter implements IGtwGlobalFilter<ServerResponse, ServerResponse> {

    @Override
    @NonNull
    public ServerResponse filter(@NonNull ServerRequest request, HandlerFunction<ServerResponse> next)
            throws Exception {
        log.info("FooDemoFilter is working for: {}", request.uri());
        return next.handle(request);
    }

}
