package com.apzda.cloud.gsvc.core;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;

/**
 * 本地导出（默认或引用）服务
 *
 * @author fengz
 */
@Slf4j
public class GatewayServiceRouteDefinitionLocator implements RouteDefinitionLocator {
    private final String appName;
    private final String serviceId;
    private final ServiceConfigurationProperties properties;
    private final int index;

    public GatewayServiceRouteDefinitionLocator(String appName,
                                                String serviceId,
                                                int index,
                                                ApplicationContext applicationContext) {
        this.appName = appName;
        this.serviceId = serviceId;
        this.index = index;
        properties = applicationContext.getBean(ServiceConfigurationProperties.class);
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        RouteDefinition routeDefinition = buildRouteDefinition();
        return Flux.just(routeDefinition);
    }

    private RouteDefinition buildRouteDefinition() {
        // 本地导出服务路由定义
        RouteDefinition routeDefinition = new RouteDefinition();
        val routeIdPrefix = "GatewayServiceFilter_" + appName + "_";
        val path = "/" + appName + "/" + serviceId;
        routeDefinition.setId(routeIdPrefix + serviceId);
        routeDefinition.setUri(URI.create("svc://" + appName + "@" + serviceId));

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg(PathRoutePredicateFactory.PATTERN_KEY, path + "/**");
        routeDefinition.getPredicates().add(predicate);

        var filter = new FilterDefinition();
        filter.setName("RewritePath");
        filter.addArg(RewritePathGatewayFilterFactory.REGEXP_KEY, path + "/?(?<segment>.+)");
        filter.addArg(RewritePathGatewayFilterFactory.REPLACEMENT_KEY, "/$\\{segment}");
        routeDefinition.getFilters().add(filter);

        val referenceConfig = this.properties.get(index);
        // 服务
        if (referenceConfig != null) {
            val connectTimeout = referenceConfig.getConnectTimeout();
            val readTimeout = referenceConfig.getReadTimeout();
            val meta = new HashMap<String, Object>();
            if (!connectTimeout.isZero()) {
                meta.put(CONNECT_TIMEOUT_ATTR, connectTimeout.toMillis());
            }
            if (!readTimeout.isZero()) {
                meta.put(RESPONSE_TIMEOUT_ATTR, readTimeout.toMillis());
            }
            meta.put("appName", referenceConfig.getApp());
            routeDefinition.setMetadata(meta);

            for (FilterDefinition original : referenceConfig.getFilters()) {
                filter = new FilterDefinition();
                filter.setName(original.getName());
                if ("RewritePath".equals(original.getName())) {
                    continue;
                }
                for (Map.Entry<String, String> entry : original.getArgs().entrySet()) {
                    filter.addArg(entry.getKey(), entry.getValue());
                }
                routeDefinition.getFilters().add(filter);
            }
        }

        return routeDefinition;
    }
}
