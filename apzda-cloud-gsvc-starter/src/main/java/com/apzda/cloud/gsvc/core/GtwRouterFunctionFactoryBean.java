package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.gtw.GroupRoute;
import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import com.apzda.cloud.gsvc.gtw.Route;
import com.apzda.cloud.gsvc.server.ServiceMethodHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.*;

import java.util.Collections;
import java.util.List;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class GtwRouterFunctionFactoryBean
        implements FactoryBean<RouterFunction<ServerResponse>>, ApplicationContextAware {

    private final GroupRoute groupRoute;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RouterFunction<ServerResponse> getObject() throws Exception {
        val routeBuilder = RouterFunctions.route();
        val interfaceName = groupRoute.getInterfaceName();
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(interfaceName);
        val routes = groupRoute.getRoutes();
        val svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        val globalFilters = svcConfigure.getGlobalFilters();

        val gMethod = groupRoute.getMethod();
        if (gMethod != null) {
            setupGroupRoute(routeBuilder, groupRoute, serviceInfo);
        }

        if (!CollectionUtils.isEmpty(routes)) {
            routeBuilder.path(groupRoute.getPath(), builder -> {
                for (Route subRoute : routes) {
                    val actions = subRoute.getActions();
                    val serviceMethod = getServiceMethod(subRoute, serviceInfo);
                    val path = subRoute.getPath();
                    val meta = subRoute.meta();
                    if (meta.isLogin()) {
                        GatewayServiceRegistry.registerRouteMeta(groupRoute.getPath() + path, meta);
                    }

                    builder.path(path, subBuilder -> {

                        if (log.isDebugEnabled()) {
                            log.debug("SN Route {}{} to {}.{}", groupRoute.getPath(), path,
                                    serviceMethod.getServiceName(), serviceMethod.getDmName());
                        }

                        HandlerFunction<ServerResponse> func = request -> ServiceMethodHandler.handle(request, "gtw",
                                serviceMethod, applicationContext);

                        for (HttpMethod action : actions) {
                            if (action == HttpMethod.GET) {
                                subBuilder.GET(func);
                            }
                            else if (action == HttpMethod.POST) {
                                subBuilder.POST(func);
                            }
                            else if (action == HttpMethod.DELETE) {
                                subBuilder.DELETE(func);
                            }
                            else if (action == HttpMethod.PUT) {
                                subBuilder.PUT(func);
                            }
                            else if (action == HttpMethod.PATCH) {
                                subBuilder.PATCH(func);
                            }
                        }

                        setupFilter(subRoute, subBuilder, Collections.emptyList());
                    });
                }
            });
        }

        setupFilter(groupRoute, routeBuilder, globalFilters);
        val exceptionHandler = applicationContext.getBean(GsvcExceptionHandler.class);
        // bookmark exception handle(gtw call)
        return routeBuilder.onError(Exception.class, exceptionHandler::handle).build();
    }

    private void setupGroupRoute(RouterFunctions.Builder builder, GroupRoute route, ServiceInfo serviceInfo) {
        val actions = route.getActions();
        val serviceMethod = getServiceMethod(route, serviceInfo);
        val path = route.getPath();
        val meta = route.meta();

        log.debug("SN Route {} to {}.{}({})", route.getPath(), serviceMethod.getServiceName(),
                serviceMethod.getDmName(), meta);

        if (meta.isLogin()) {
            GatewayServiceRegistry.registerRouteMeta(path, meta);
        }

        HandlerFunction<ServerResponse> func = request -> ServiceMethodHandler.handle(request, "gtw", serviceMethod,
                applicationContext);

        for (HttpMethod action : actions) {
            if (action == HttpMethod.GET) {
                builder.GET(path, func);
            }
            else if (action == HttpMethod.POST) {
                builder.POST(path, func);
            }
            else if (action == HttpMethod.DELETE) {
                builder.DELETE(path, func);
            }
            else if (action == HttpMethod.PUT) {
                builder.PUT(path, func);
            }
            else if (action == HttpMethod.PATCH) {
                builder.PATCH(path, func);
            }
        }
    }

    private ServiceMethod getServiceMethod(Route route, ServiceInfo serviceInfo) {
        val method = route.getMethod();
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(serviceInfo);
        val serviceMethod = methods.get(method);
        if (serviceMethod == null) {
            throw new IllegalStateException(String.format("method of %s is not found", route));
        }
        return serviceMethod;
    }

    @SuppressWarnings("unchecked")
    private void setupFilter(Route route, RouterFunctions.Builder builder,
            List<IGtwGlobalFilter<ServerResponse, ServerResponse>> globalFilters) {
        for (HandlerFilterFunction<ServerResponse, ServerResponse> filter : globalFilters) {
            builder.filter(filter);
        }
        var filters = route.getFilters();

        if (filters.isEmpty()) {
            return;
        }

        log.debug("Setup filters for {}", route);

        val filtersBean = filters.stream()
            .map(filter -> applicationContext.getBean(filter, HandlerFilterFunction.class))
            .toList();

        if (!CollectionUtils.isEmpty(filtersBean)) {
            for (HandlerFilterFunction<ServerResponse, ServerResponse> filter : filtersBean) {
                builder.filter(filter);
            }
        }
    }

    @Override
    public Class<?> getObjectType() {
        return RouterFunction.class;
    }

}
