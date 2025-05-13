package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.gtw.ProxyExchangeHandler;
import com.apzda.cloud.gsvc.gtw.Route;
import com.apzda.cloud.gsvc.gtw.RouteRegistry;
import com.apzda.cloud.gsvc.server.IServiceMethodHandler;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.apzda.cloud.gsvc.gtw.ProxyExchangeHandler.parseUri;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class GtwRouterFunctionFactoryBean
        implements FactoryBean<RouterFunction<ServerResponse>>, ApplicationContextAware {

    public static final String ATTR_MATCHED_SEGMENTS = "GSVC_ATTR_MATCHED_SEGMENTS";

    private static final Logger webLog = LoggerFactory.getLogger(GtwRouterFunctionFactoryBean.class);

    private final Route route;

    private final Class<?> serviceClass;

    @Value("${server.servlet.context-path:/}")
    private String servletContext;

    private ApplicationContext applicationContext;

    private ProxyExchangeHandler proxyExchangeHandler;

    private IServiceMethodHandler serviceMethodHandler;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.proxyExchangeHandler = applicationContext.getBean(ProxyExchangeHandler.class);
        this.serviceMethodHandler = applicationContext.getBean(IServiceMethodHandler.class);
    }

    @Override
    public RouterFunction<ServerResponse> getObject() throws Exception {
        servletContext = StringUtils.stripEnd(servletContext, "/");
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(serviceClass);
        val router = RouterFunctions.route();
        if ("http".equals(serviceInfo.type)) {
            setupForward(router, route, serviceInfo);
            setupFilter(router, route);
        }
        else if (StringUtils.startsWith(route.getMethod(), "/")) {
            if (webLog.isWarnEnabled()) {
                webLog.warn("Route {} to {}.{}({}) skipped.", route.absPath(), serviceInfo.getServiceName(),
                        route.getMethod(), route.meta());
            }
            // prefixed .ig for response 404
            router.path(".ig" + route.absPath(), () -> request -> {
                throw new ErrorResponseException(HttpStatus.NOT_FOUND);
            });
        }
        else {
            setupRoute(router, route, serviceInfo);
            setupFilter(router, route);
        }

        val meta = route.meta();
        if (meta.isLogin()) {
            val path = route.absPath();
            GatewayServiceRegistry.registerRouteMeta(path, meta);
        }

        val exceptionHandler = applicationContext.getBean(GsvcExceptionHandler.class);
        // bookmark exception handle(gtw call)
        return router.onError(Exception.class, exceptionHandler::handle).build();
    }

    private void setupRoute(@NonNull RouterFunctions.Builder builder, @NonNull Route route, ServiceInfo serviceInfo) {
        val actions = route.getActions();
        val consumes = Optional.ofNullable(route.getConsumes())
            .map(c -> Stream.of(c).map(MediaType::valueOf).toList())
            .orElse(Collections.emptyList());
        val excludes = Arrays.stream(route.getExcludes()).toList();
        val serviceMethod = getServiceMethod(route, serviceInfo);
        val path = route.absPath();

        if (serviceMethod == null) {
            builder.path(".ig" + path, () -> request -> {
                throw new ErrorResponseException(HttpStatus.NOT_FOUND);
            });
            return;
        }

        val method = serviceMethod.getDmName();
        if (webLog.isDebugEnabled()) {
            webLog.debug("SN Route {} to {}.{}({})", path, serviceMethod.getServiceName(), method, route.meta());
        }

        HandlerFunction<ServerResponse> func = request -> {
            ServiceMethod realServiceMethod = serviceMethod;
            String dmName = realServiceMethod.getDmName();

            if ("*".equals(method)) {
                val pattern = route.getMethod();
                val upper = !StringUtils.startsWith(pattern, "{");
                dmName = parseUri(request, pattern, upper);
                try {
                    realServiceMethod = getServiceMethod(dmName, serviceInfo);
                }
                catch (IllegalStateException e) {
                    throw new ErrorResponseException(HttpStatus.NOT_FOUND);
                }
            }

            val type = realServiceMethod.getType();

            return switch (type) {
                case UNARY -> serviceMethodHandler.handleUnary(request, serviceClass, dmName, null);
                case SERVER_STREAMING ->
                    serviceMethodHandler.handleServerStreaming(request, serviceClass, dmName, null);
                default -> serviceMethodHandler.handleBidStreaming(request, serviceClass, dmName, null);
            };
        };

        builder.route((request -> match(request, path, actions, consumes, excludes)), func);
    }

    private void setupForward(RouterFunctions.Builder builder, Route route, ServiceInfo serviceInfo) {
        val actions = route.getActions();
        val path = route.absPath();
        val consumes = Optional.ofNullable(route.getConsumes())
            .map(c -> Stream.of(c).map(MediaType::valueOf).toList())
            .orElse(Collections.emptyList());
        val excludes = Arrays.stream(route.getExcludes()).toList();

        if (webLog.isDebugEnabled()) {
            webLog.debug("FW Route {} to {}.{}({})", path, serviceInfo.getServiceName(), route.getMethod(),
                    route.meta());
        }

        final HandlerFunction<ServerResponse> func = request -> proxyExchangeHandler.handle(request, route,
                serviceInfo);

        builder.route(request -> match(request, path, actions, consumes, excludes), func);

        RouteRegistry.register(path);
    }

    @SuppressWarnings("unchecked")
    private void setupFilter(RouterFunctions.Builder router, Route route) {
        val svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        val globalFilters = svcConfigure.getGlobalFilters();
        if (!globalFilters.isEmpty()) {
            webLog.trace("Setup global filters for {}: {}", route, globalFilters);
            for (HandlerFilterFunction<ServerResponse, ServerResponse> filter : globalFilters) {
                router.filter(filter);
            }
        }
        var filters = route.getFilters();

        if (filters.isEmpty()) {
            return;
        }

        webLog.trace("Setup filters for {}: {}", route, filters);

        val filtersBean = filters.stream()
            .map(filter -> applicationContext.getBean(filter, HandlerFilterFunction.class))
            .toList();

        if (!CollectionUtils.isEmpty(filtersBean)) {
            for (HandlerFilterFunction<ServerResponse, ServerResponse> filter : filtersBean) {
                router.filter(filter);
            }
        }
    }

    private ServiceMethod getServiceMethod(Route route, ServiceInfo serviceInfo) {
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(serviceInfo);
        val method = route.getMethod();

        if (StringUtils.endsWith(method, "}")) {
            if (methods.isEmpty()) {
                return null;
            }
            val serviceMethod = methods.values().stream().toList().get(0);
            return new ServiceMethod(serviceMethod, "*");
        }

        return getServiceMethod(method, serviceInfo);
    }

    @Nonnull
    private ServiceMethod getServiceMethod(String method, ServiceInfo serviceInfo) {
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(serviceInfo);
        val serviceMethod = methods.get(method);
        if (serviceMethod == null) {
            throw new IllegalStateException(String.format("method of %s is not found", route));
        }
        return serviceMethod;
    }

    @Override
    public Class<?> getObjectType() {
        return RouterFunction.class;
    }

    private boolean match(ServerRequest request, String path, List<HttpMethod> actions, List<MediaType> consumes,
            List<String> excludes) {
        val reqPath = request.path();

        boolean matched = RouteRegistry.pathMatcher.match(path, reqPath);

        if (matched && !actions.isEmpty()) {
            matched = actions.contains(request.method());
        }

        if (matched && !consumes.isEmpty()) {
            val contentType = request.headers().contentType();
            if (contentType.isEmpty()) {
                return false;
            }
            val mediaType = contentType.get();
            if (consumes.parallelStream().noneMatch(mediaType::isCompatibleWith)) {
                return false;
            }
        }

        val httpServletRequest = request.servletRequest();

        if (matched && httpServletRequest.getAttribute(ATTR_MATCHED_SEGMENTS) == null) {
            val segments = RouteRegistry.pathMatcher.extractUriTemplateVariables(path, reqPath);
            val segment = RouteRegistry.pathMatcher.extractPathWithinPattern(path, reqPath);
            if (excludes.contains(segment)) {
                return false;
            }
            segments.put("segment", segment);
            httpServletRequest.setAttribute(ATTR_MATCHED_SEGMENTS, segments);
            httpServletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, path);
            webLog.trace("{} matched '{}' with segments: {}", reqPath, path, segments);
        }
        return matched;
    }

}
