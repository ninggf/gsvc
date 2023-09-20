package com.apzda.cloud.gsvc.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import reactor.core.publisher.Flux;

/**
 * @author fengz windywany@gmail.com
 */
public interface IServiceCaller {

    <T, R> R unaryCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz);

    <T, R> Flux<R> serverStreamingCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz);

    default <T, R> R clientStreamingCall(Class<?> clazz, String method, Flux<T> request, Class<T> reqClazz,
            Class<R> resClazz) {
        throw new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED,
                String.format("'%s' is client-streaming method which not supported by webclient now", method));
    }

    default <T, R> Flux<R> bidiStreamingCall(Class<?> clazz, String method, Flux<T> request, Class<T> reqClazz,
            Class<R> resClazz) {
        throw new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED,
                String.format("'%s' is bidi-streaming method which not supported by webclient now", method));
    }

}
