package com.apzda.cloud.gsvc.filter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * @author fengz
 */
@Slf4j
public class ExporterGatewayFilterFactory extends AbstractGatewayFilterFactory<ExporterGatewayFilterFactory.Config> {
    public static final String EXPORTS_KV_KEY = "exports";
    public static final String EXPORTED = "method-exported";

    public ExporterGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of(EXPORTS_KV_KEY);
    }

    @Override
    public Config newConfig() {
        return new Config();
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public GatewayFilter apply(Config config) {
        val exports = config.getReverseMap();
        if (log.isInfoEnabled()) {
            log.info("Export methods for {}: {}", config.getRouteId(), exports);
        }
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                final Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
                if (route != null) {
                    String matchedRouteId = exchange.getAttribute(GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);
                    String matchedPath = exchange.getAttribute(GATEWAY_PREDICATE_MATCHED_PATH_ATTR);
                    exchange.getAttributes().put(EXPORTED, false);
                    if (route.getId()
                             .equals(matchedRouteId) && StringUtils.hasText(matchedPath) && !CollectionUtils.isEmpty(
                            exports)) {
                        val req = exchange.getRequest();

                        val path = req.getURI().getRawPath();
                        val modifier = switch (req.getMethod().name()) {
                            case "GET" -> "-";
                            case "POST" -> "+";
                            default -> "*";
                        };

                        val methodKey = modifier + path.substring(StringUtils.trimTrailingCharacter(matchedPath, '*')
                                                                             .length());

                        if (!exports.containsKey(methodKey)) {
                            if (log.isDebugEnabled()) {
                                log.debug("No method of {} exports for {}", req.getURI().getHost(), path);
                            }
                            return chain.filter(exchange);
                        }

                        val serviceMethod = "/" + exports.get(methodKey);
                        if (log.isDebugEnabled()) {
                            log.debug("Rewriting {} to {}", path, route.getUri() + serviceMethod);
                        }
                        // 仅在修改了URI的情况下才需要保存
                        addOriginalRequestUrl(exchange, req.getURI());

                        val request = req.mutate().path(serviceMethod).build();

                        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, request.getURI());
                        exchange.getAttributes().put(EXPORTED, true); // 导出方法
                        return chain.filter(exchange.mutate().request(request).build());
                    }
                }
                return chain.filter(exchange);
            }

            @Override
            public String toString() {
                ToStringCreator toStringCreator = filterToStringCreator(ExporterGatewayFilterFactory.this);
                for (KeyValue keyValue : config.getExports()) {
                    toStringCreator.append(keyValue.key, keyValue.value);
                }
                return toStringCreator.toString();
            }
        };
    }

    @Getter
    public static class Config implements HasRouteId {
        private final Map<String, String> reverseMap = new HashMap<>();
        private String routeId;
        private KeyValue[] exports;

        @Override
        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }

        @Override
        public String getRouteId() {
            return routeId;
        }


        public void setExports(KeyValue[] exports) {
            this.exports = exports;
            for (KeyValue keyValue : exports) {
                reverse(keyValue.key, keyValue.value);
            }
        }

        private void reverse(String key, String value) {
            if (value.startsWith("-") || value.startsWith("+")) {
                if (value.length() == 1) {
                    reverseMap.putIfAbsent(value + key, key);
                } else {
                    reverseMap.putIfAbsent(value, key);
                }
            } else if (value.startsWith("*")) {
                if (value.length() == 1) {
                    value = value + key;
                }
                value = value.substring(1);
                reverseMap.putIfAbsent("+" + value, key);
                reverseMap.putIfAbsent("-" + value, key);
            } else {
                reverseMap.putIfAbsent("+" + value, key);
                reverseMap.putIfAbsent("-" + value, key);
            }
        }
    }

    public record KeyValue(String key, String value) {

        @Override
        public String toString() {
            return new ToStringCreator(this).append("name", key).append("value", value).toString();
        }
    }
}
