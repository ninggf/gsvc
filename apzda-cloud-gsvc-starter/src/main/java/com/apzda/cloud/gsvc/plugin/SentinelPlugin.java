package com.apzda.cloud.gsvc.plugin;

import com.alibaba.csp.sentinel.adapter.reactor.SentinelReactorTransformer;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.exception.DegradedException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

/**
 * @author fengz
 */
@Slf4j
public class SentinelPlugin implements IGlobalPlugin, IPostCall {

    @Override
    public <R> Mono<R> postCall(Mono<R> response, ServiceMethod method, Class<R> rClass) {
        val resource = String.format("/~%s/%s/%s", method.getCfgName(), method.getServiceName(), method.getDmName());

        // log.trace("Register Sentinel Resource: {}", resource);

        return response.transform(new SentinelReactorTransformer<>(resource))
            .onErrorMap(DegradeException.class, e -> new DegradedException(e.getMessage()));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10000;
    }

}
