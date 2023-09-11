package com.apzda.cloud.gsvc.plugin;

import com.alibaba.csp.sentinel.adapter.reactor.SentinelReactorTransformer;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import reactor.core.publisher.Mono;

/**
 * @author fengz
 */
@Slf4j
public class SentinelPlugin implements IGlobalPlugin, IPostCall {

    @Override
    public <R> Mono<R> postCall(Mono<R> response, ServiceMethod method, Class<R> rClass) {
        val resource = String.format("/~%s/%s/%s", method.getSvcLbName(), method.getServiceName(), method.getDmName());

        log.trace("Register Sentinel Resource: {}", resource);

        return response.transform(new SentinelReactorTransformer<>(resource))
            .onErrorReturn(DegradeException.class,
                    ResponseUtils.fallback(ServiceError.DEGRADE, method.getServiceName(), rClass));
    }

}
