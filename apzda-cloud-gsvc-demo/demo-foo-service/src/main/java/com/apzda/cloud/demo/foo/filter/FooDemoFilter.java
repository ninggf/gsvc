package com.apzda.cloud.demo.foo.filter;

import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import lombok.extern.slf4j.Slf4j;
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
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        log.info("FooDemoFilter is working");
        return next.handle(request);
    }

}
