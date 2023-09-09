package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.gtw.GroupRoute;
import com.apzda.cloud.gsvc.gtw.Route;
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

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class GroupRouterFunctionFactoryBean
        implements FactoryBean<RouterFunction<ServerResponse>>, ApplicationContextAware {

    private final GroupRoute groupRoute;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RouterFunction<ServerResponse> getObject() throws Exception {
        val route = RouterFunctions.route();
        val interfaceName = groupRoute.getInterfaceName();
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(interfaceName);
        val routes = groupRoute.getRoutes();

        if (CollectionUtils.isEmpty(routes)) {
            setupGroupRoute(route, groupRoute, serviceInfo);
        }
        else {
            val gMethod = groupRoute.getMethod();
            if (gMethod != null) {
                setupGroupRoute(route, groupRoute, serviceInfo);
            }

            route.path(groupRoute.getPath(), builder -> {
                for (Route subRoute : routes) {
                    val actions = subRoute.getActions();
                    val serviceMethod = getServiceMethod(subRoute, serviceInfo);
                    builder.path(subRoute.getPath(), subBuilder -> {

                        if (log.isDebugEnabled()) {
                            log.debug("SN Route {}{} to {}@{}/{}", groupRoute.getPath(), subRoute.getPath(),
                                    serviceMethod.getAppName(), serviceMethod.getServiceName(),
                                    serviceMethod.getDmName());
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
                        setupFilter(subRoute, subBuilder);
                    });
                }
            });
        }

        setupFilter(groupRoute, route);
        val exceptionHandler = applicationContext.getBean(GsvcExceptionHandler.class);
        return route.onError(Exception.class, exceptionHandler::handle).build();
    }

    private void setupGroupRoute(RouterFunctions.Builder builder, GroupRoute route,
            GatewayServiceRegistry.ServiceInfo serviceInfo) {
        val actions = route.getActions();
        val serviceMethod = getServiceMethod(route, serviceInfo);
        val path = route.getPath();
        log.debug("SN Route {} to {}@{}/{}", route.getPath(), serviceMethod.getAppName(),
                serviceMethod.getServiceName(), serviceMethod.getDmName());

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

    private GatewayServiceRegistry.ServiceMethod getServiceMethod(Route route,
            GatewayServiceRegistry.ServiceInfo serviceInfo) {
        val method = route.getMethod();
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(serviceInfo);
        val serviceMethod = methods.get(method);
        if (serviceMethod == null) {
            throw new IllegalStateException(String.format("method of %s is not found", route));
        }
        return serviceMethod;
    }

    @SuppressWarnings("unchecked")
    private void setupFilter(Route route, RouterFunctions.Builder builder) {
        var filters = route.getFilters();

        if (route.parent() != null) {
            // 添加通用过滤器
        }

        if (route.getLogin()) {
            filters.add("login");
        }
        else {
            filters.remove("login");
        }

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
