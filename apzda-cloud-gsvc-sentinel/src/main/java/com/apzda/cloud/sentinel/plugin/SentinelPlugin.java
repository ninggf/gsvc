package com.apzda.cloud.sentinel.plugin;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.adapter.reactor.ContextConfig;
import com.alibaba.csp.sentinel.adapter.reactor.EntryConfig;
import com.alibaba.csp.sentinel.adapter.reactor.SentinelReactorTransformer;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.SentinelWebInterceptor;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.apzda.cloud.gsvc.core.ServiceInfo;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.exception.DegradedException;
import com.apzda.cloud.gsvc.plugin.IForwardPlugin;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPostCall;
import io.micrometer.common.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;

/**
 * @author fengz
 */
@Slf4j
public class SentinelPlugin implements IGlobalPlugin, IPostCall, IForwardPlugin {

    private final static ContextConfig contextConfig = new ContextConfig(
            SentinelWebInterceptor.SENTINEL_SPRING_WEB_CONTEXT_NAME);

    @Override
    public <R> Flux<R> postForward(@NonNull ServiceInfo serviceInfo, Flux<R> response, String uri, HttpMethod method) {
        if (!StringUtils.startsWith(uri, "/~")) {
            uri = "@" + serviceInfo.getServiceName() + uri;
        }
        val entryConfig = new EntryConfig(method.name() + ":" + uri, EntryType.OUT, contextConfig);
        return response.transform(new SentinelReactorTransformer<>(entryConfig))
            .onErrorMap(DegradeException.class, e -> new DegradedException(e.getMessage()));
    }

    @Override
    public <R> Flux<R> postCall(@NonNull ServiceInfo serviceInfo, Flux<R> response, ServiceMethod method,
            Class<R> rClass) {
        val resource = String.format("/~%s/%s", method.getServiceName(), method.getDmName());

        // log.trace("Register Sentinel Resource: {}", resource);

        return postForward(serviceInfo, response, resource, HttpMethod.POST);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10000;
    }

}
