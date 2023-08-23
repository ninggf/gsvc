package com.apzda.cloud.gsvc.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.SaTokenExtendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.SocketException;
import java.net.URI;

import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * 本过滤器在{@link org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter}之前运行.
 * 本过滤器处理路由规则中<em>uri</em>的<em>schema</em>为<em>svc</em>的请求.
 * 对服务的要求如下:
 * 1. 服务的实现不在本地且被{@link ExporterGatewayFilterFactory}插件导出。
 * 对满足要求的服务转发至<em>LoadBalancer</em>.
 * 本类还有一个职责: 将请求转换成请求对象
 *
 * @author ninggf
 */
@Slf4j
@RequiredArgsConstructor
public class ReferenceServiceFilter implements GlobalFilter, Ordered {
    private static final String PERCENTAGE_SIGN = "%";
    private final SaTokenExtendProperties properties;

    private static boolean containsEncodedParts(URI uri) {
        //@formatter:off
        boolean encoded = (uri.getRawQuery() != null && uri.getRawQuery().contains(PERCENTAGE_SIGN))
                || (uri.getRawPath() != null && uri.getRawPath().contains(PERCENTAGE_SIGN))
                || (uri.getRawFragment() != null && uri.getRawFragment().contains(PERCENTAGE_SIGN));
        //@formatter:on
        if (encoded) {
            try {
                UriComponentsBuilder.fromUri(uri).build(true);
                return true;
            } catch (IllegalArgumentException ignore) {
            }
            return false;
        }
        return false;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取最新URL
        final Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        final URI url = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (route == null || isAlreadyRouted(exchange) || !"svc".equals(url.getScheme())) {
            return chain.filter(exchange);
        }
        log.trace("ReferenceServiceFilter start");

        val serviceName = url.getHost();
        val app = StringUtils.defaultIfBlank(route.getUri().getUserInfo(), serviceName);
        val hasLocalImpl = GatewayServiceRegistry.isLocalService(app, serviceName);
        val exported = exchange.getAttribute(ExporterGatewayFilterFactory.EXPORTED);
        val req = exchange.getRequest();
        // 服务不在本地且导出 -> LoadBalancer
        if (!hasLocalImpl && Boolean.TRUE.equals(exported)) {
            // 仅在修改了URI的情况下才需要保存
            addOriginalRequestUrl(exchange, req.getURI());
            val encoded = containsEncodedParts(url);
            val lbUri = UriComponentsBuilder.fromUri(url)
                .scheme("lb")
                .host(app)
                .replacePath("/" + app + "/" + serviceName + req.getPath().value())
                .build(encoded)
                .toUri();

            log.debug("Rewriting {} to {}", url, lbUri);

            val request = req.mutate().uri(lbUri).build();
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, request.getURI());
            return checkLogin(exchange.mutate().request(request).build(),
                app,
                serviceName,
                req.getPath().value(),
                chain)
                .doOnError(tx -> log.warn("Call upstream for '{}' failed: {}", url, tx.getMessage()))
                .onErrorMap(SocketException.class,
                    tx -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, tx.getMessage(), tx));
        }

        return checkLogin(exchange, app, serviceName, req.getPath().value(), chain);
    }

    @Override
    public int getOrder() {
        // before load balancer client filter running
        return LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;
    }

    private Mono<Void> checkLogin(ServerWebExchange exchange,
                                  String app,
                                  String service,
                                  String path,
                                  GatewayFilterChain chain) {
        val method = StringUtils.strip(path, "/ ");
        val mInfo = GatewayServiceRegistry.getServiceMethod(app, service, method);
        if (mInfo == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)).then(chain.filter(exchange));
        }

        if (mInfo.getCurrentUserClz() != null) {
            try {
                StpUtil.checkLogin();
            } catch (Exception e) {
                setAlreadyRouted(exchange);// very import
                log.debug("用户未登录: {}", e.getMessage());
                val contentTypes = exchange.getRequest().getHeaders().getAccept();
                val loginUrl = properties.getLoginUrl();
                val textType = MediaType.parseMediaType("text/*");
                if (loginUrl == null || CollectionUtils.isEmpty(contentTypes)) {
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).then(chain.filter(exchange));
                } else {
                    var notRedirect = true;
                    for (MediaType contentType : contentTypes) {
                        if (contentType.isCompatibleWith(textType)) {
                            // redirect to the login page
                            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                            exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, loginUrl.toString());
                            notRedirect = false;
                            break;
                        }
                    }
                    if (notRedirect) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).then(chain.filter(exchange));
                    }
                }
            }
        }

        return chain.filter(exchange);
    }
}
