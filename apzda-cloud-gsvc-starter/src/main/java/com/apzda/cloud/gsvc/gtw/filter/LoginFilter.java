package com.apzda.cloud.gsvc.gtw.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

@Slf4j
public class LoginFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    @Override
    public ServerResponse filter(@NonNull ServerRequest request, @NonNull HandlerFunction<ServerResponse> next)
            throws Exception {
        log.debug("apply login filter!!!");
        return next.handle(request);
    }

}
