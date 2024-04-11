package com.apzda.cloud.gsvc.server;

import build.buf.protovalidate.Validator;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class DefaultServiceMethodHandler implements IServiceMethodHandler {

    private final GatewayServiceConfigure svcConfigure;

    private final ObjectMapper objectMapper;

    private final GsvcExceptionHandler exceptionHandler;

    private final Validator validator;

    private final MultipartResolver multipartResolver;

    @Override
    public ServerResponse handleUnary(ServerRequest request, Class<?> serviceClz, String method, Function<? extends Message, ? extends Message> func) {
        return null;
    }

    @Override
    public ServerResponse handleServerStreaming(ServerRequest request, Class<?> serviceClz, String method, Function<? extends Message, Flux<? extends Message>> func) {
        return null;
    }

    @Override
    public ServerResponse handleBidStreaming(ServerRequest request, Class<?> serviceClz, String method, Function<Flux<? extends Message>, Flux<? extends Message>> func) {
        return null;
    }
}
