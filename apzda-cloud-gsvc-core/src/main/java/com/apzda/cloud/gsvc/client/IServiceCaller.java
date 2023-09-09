package com.apzda.cloud.gsvc.client;

import reactor.core.publisher.Mono;

/**
 * @author fengz windywany@gmail.com
 */
public interface IServiceCaller {

    <T, R> R unaryCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz);

    <T, R> Mono<R> serverStreamingCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz);

    <T, R> Mono<R> bidiStreamingCall(Class<?> clazz, String method, Mono<T> request, Class<T> reqClazz,
            Class<R> resClazz);

}
