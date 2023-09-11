/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import reactor.core.publisher.Mono;

/**
 * @author fengz
 */
public interface IPostCall extends IPlugin {

    <R> Mono<R> postCall(Mono<R> response, ServiceMethod method, Class<R> rClass);

}
