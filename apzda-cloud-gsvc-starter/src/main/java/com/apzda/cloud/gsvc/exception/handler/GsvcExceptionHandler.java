package com.apzda.cloud.gsvc.exception.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * @author ninggf
 */
@Slf4j
public class GsvcExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("ex: {}", ex.getMessage());
        return Mono.<Void>error(ex).checkpoint("[GsvcExceptionHandler]",true);
    }
}
