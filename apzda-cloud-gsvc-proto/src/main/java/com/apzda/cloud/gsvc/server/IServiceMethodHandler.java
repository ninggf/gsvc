package com.apzda.cloud.gsvc.server;

import com.google.protobuf.Message;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public interface IServiceMethodHandler {

    // unary
    ServerResponse handleUnary(ServerRequest request, Class<?> serviceClz, String method,
            Function<? extends Message, ? extends Message> func);

    // server streaming
    ServerResponse handleServerStreaming(ServerRequest request, Class<?> serviceClz, String method,
            Function<? extends Message, Flux<? extends Message>> func);

    // bid streaming
    ServerResponse handleBidStreaming(ServerRequest request, Class<?> serviceClz, String method,
            Function<Flux<? extends Message>, Flux<? extends Message>> func);

}
