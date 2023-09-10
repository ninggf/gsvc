/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;


/**
 * @author fengz
 */
public interface IPostPlugin extends IPlugin {

    <R> Mono<R> postCall(Mono<R> response, ServiceMethod method, ApplicationContext context);

}
