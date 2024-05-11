package com.apzda.cloud.gsvc.server;

import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.function.Function;

public interface IServiceMethodHandler {

    String GTW = "g-t-w";

    String CALLER_HEADER = "X-Gsvc-Caller";

    // unary
    ServerResponse handleUnary(ServerRequest request, Class<?> serviceClz, String method,
            Function<Object, Object> func);

    // server streaming
    ServerResponse handleServerStreaming(ServerRequest request, Class<?> serviceClz, String method,
            Function<Object, Object> func);

    // bid streaming
    ServerResponse handleBidStreaming(ServerRequest request, Class<?> serviceClz, String method,
            Function<Object, Object> func);

}
