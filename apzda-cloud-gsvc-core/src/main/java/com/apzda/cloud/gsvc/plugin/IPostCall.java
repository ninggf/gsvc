/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceInfo;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import io.micrometer.common.lang.NonNull;
import reactor.core.publisher.Flux;

/**
 * @author fengz
 */
public interface IPostCall extends IPlugin {

    <R> Flux<R> postCall(@NonNull ServiceInfo serviceInfo, Flux<R> response, ServiceMethod method, Class<R> rClass);

}
