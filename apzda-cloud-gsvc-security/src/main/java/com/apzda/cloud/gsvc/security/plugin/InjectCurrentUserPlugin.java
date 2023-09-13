/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.apzda.cloud.gsvc.security.IUser;
import com.fasterxml.jackson.databind.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.context.SecurityContextHolder;
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
        if (method.getCurrentUserClz() != null) {
            val context = SecurityContextHolder.getContext();
            if (!context.getAuthentication().isAuthenticated()) {
                return data;
            }
            val principal = context.getAuthentication().getPrincipal();
            if (!(principal instanceof IUser)) {
                return data;
            }
            val currentUser = new CurrentUser();
            if (data instanceof Mono<?>) {
                data = ((Mono<?>) data).handle((d, sink) -> {
                    sink.next(d);
                    sink.complete();
                });
            }
            else {

            }
        }
        return data;
    }

}
