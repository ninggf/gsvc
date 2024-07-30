/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.seata.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.fasterxml.jackson.databind.JsonNode;
import io.seata.core.context.RootContext;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class SeataPlugin implements IGlobalPlugin, IPreCall, IPreInvoke {

    @Override
    @Nonnull
    public WebClient.RequestBodySpec preCall(@Nonnull WebClient.RequestBodySpec request, Object data,
            ServiceMethod method) {
        val xid = RootContext.getXID();
        if (StringUtils.isNotBlank(xid)) {
            return request.headers(httpHeaders -> {
                log.debug("xid in RootContext {}", xid);
                httpHeaders.add(RootContext.KEY_XID, xid);
            });
        }
        else {
            return request;
        }
    }

    @Override
    public Mono<JsonNode> preInvoke(ServerRequest request, Mono<JsonNode> data, ServiceMethod method) {
        try {
            String xid = RootContext.getXID();
            String rpcXid = request.headers().asHttpHeaders().getFirst(RootContext.KEY_XID);
            if (log.isDebugEnabled()) {
                log.debug("xid in RootContext {} xid in RpcContext {}", xid, rpcXid);
            }

            if (StringUtils.isBlank(xid) && StringUtils.isNotBlank(rpcXid)) {
                RootContext.bind(rpcXid);
                if (log.isDebugEnabled()) {
                    log.debug("bind {} to RootContext", rpcXid);
                }
            }
        }
        catch (Exception ignored) {
        }

        return data;
    }

}
