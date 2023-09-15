/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * @author fengz windywany@gmail.com
 **/
@Slf4j
public class InjectCurrentUserPlugin implements IGlobalPlugin, IPreInvoke, IPreCall {

    // private final ObjectMapper objectMapper;
    @Override
    public WebClient.RequestBodySpec preCall(WebClient.RequestBodySpec request, Mono<Object> data,
            ServiceMethod method) {

        return request;
    }

    @Override
    public Object preInvoke(ServerRequest request, Object data, ServiceMethod method) {
        return data;
    }

}
