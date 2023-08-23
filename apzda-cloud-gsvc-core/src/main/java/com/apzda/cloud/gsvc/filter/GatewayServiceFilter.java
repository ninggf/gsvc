package com.apzda.cloud.gsvc.filter;

import com.apzda.cloud.gsvc.core.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GatewayServiceHandler;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter.filterRequest;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * 本过滤器处理路由规则中<em>uri</em>的<em>schema</em>为<em>svc</em>的请求.
 * 要求服务的实现在服务在本地.
 *
 * @author fengz
 */
@Slf4j
public class GatewayServiceFilter implements GlobalFilter, Ordered {
    private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;
    private final ApplicationContext applicationContext;
    private final GatewayServiceConfigure svcConfigure;
    private volatile List<HttpHeadersFilter> headersFilters;

    public GatewayServiceFilter(ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
                                ApplicationContext applicationContext) {
        this.headersFiltersProvider = headersFiltersProvider;
        this.applicationContext = applicationContext;
        svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
    }

    // 南北流量使用全局插件
    @Override
    @SuppressWarnings("Duplicates")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        final URI requestUri = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        val scheme = requestUri.getScheme();

        if (route == null || isAlreadyRouted(exchange) || !"svc".equalsIgnoreCase(scheme)) {
            return chain.filter(exchange);
        }

        setAlreadyRouted(exchange);
        val logId = exchange.getRequiredAttribute(ServerWebExchange.LOG_ID_ATTRIBUTE);
        val exported = exchange.getAttribute(ExporterGatewayFilterFactory.EXPORTED);
        val serviceName = requestUri.getHost();
        val app = StringUtils.defaultIfBlank(route.getUri().getUserInfo(), serviceName);
        val isLocalService = GatewayServiceRegistry.isLocalService(app, serviceName);

        log.trace("[{}] Applying GatewayServiceFilter for {}", logId, serviceName);

        // 服务不在本地且未导出: 手动配置svc://xxx@yyyy但未使用Exporter过滤器
        if (!isLocalService && !Boolean.TRUE.equals(exported)) {
            log.debug("[{}] The Remote Service '{}' does not export", logId, serviceName);
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)).then(chain.filter(exchange));
        }

        HttpHeaders filtered = filterRequest(getHeadersFilters(), exchange);

        val httpHeaders = new DefaultHttpHeaders();
        filtered.forEach(httpHeaders::set);

        // 路径即为服务的方法名，所以要保证纯洁
        val method = StringUtils.strip(requestUri.getPath(), "/ ");
        val mInfo = GatewayServiceRegistry.getServiceMethod(app, serviceName, method);
        if (mInfo == null) {
            log.debug("[{}] The method {} of {}@{} not found", logId, method, app, serviceName);

            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)).then(chain.filter(exchange));
        }
        log.trace("[{}] Will call service's method: {}@{}/{}", logId, app, serviceName, method);

        mInfo.setHeaders(httpHeaders);
        exchange.getAttributes().put("filtered_http_headers",httpHeaders);
        val timeout = svcConfigure.getTimeout(mInfo.getServiceIndex(), method);
        // TODO： 需要传递请求上下文
        val handler = switch (mInfo.getType()) {
            case UNARY -> Mono.fromFuture(GatewayServiceHandler.handle(mInfo, exchange, applicationContext));
            default -> GatewayServiceHandler.handleAsync(mInfo, exchange, applicationContext);
        };

        //bookmark: 设置Content-Type为json
        exchange.getResponse()
            .getHeaders()
            .set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return exchange.getResponse()
            .writeWith(handler)
            .timeout(timeout,
                Mono.error(new TimeoutException("Response took longer than timeout: " + timeout)))
            .onErrorMap(TimeoutException.class,
                th -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, th.getMessage(), th))
            .doOnSuccess((aVoid) -> {
                log.trace("[{}] 调用本地服务[{}@{}/{}]完成", logId, app, serviceName, method);
            })
            .doOnError(err -> {
                if (log.isTraceEnabled()) {
                    log.trace("[{}] 调用本地服务[{}@{}/{}]失败: {}", logId, serviceName, app, method, err.getMessage());
                }
            })
            .then(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public List<HttpHeadersFilter> getHeadersFilters() {
        if (headersFilters == null) {
            headersFilters = headersFiltersProvider.getIfAvailable();
        }
        return headersFilters;
    }
}
