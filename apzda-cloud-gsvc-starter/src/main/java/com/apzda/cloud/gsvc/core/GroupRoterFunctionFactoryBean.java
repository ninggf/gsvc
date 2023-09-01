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

@Slf4j
@RequiredArgsConstructor
public class GroupRoterFunctionFactoryBean
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
        val serviceIndex = groupRoute.getServiceIndex();
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(serviceIndex);
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(serviceInfo);
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
                        log.debug("Route {}{} to {}@{}/{}", groupRoute.getPath(), subRoute.getPath(),
                                serviceMethod.getAppName(), serviceMethod.getServiceName(), serviceMethod.getDmName());
                        HandlerFunction<ServerResponse> func = request -> ServiceMethodHandler.handle(request,
                                serviceMethod, applicationContext);
                        for (HttpMethod action : actions) {
                            if (action == HttpMethod.GET) {
                                subBuilder.GET(func);
                            }
                            else if (action == HttpMethod.POST) {
                                subBuilder.POST(func);
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
        HandlerFunction<ServerResponse> func = request -> ServiceMethodHandler.handle(request, serviceMethod,
                applicationContext);

        for (HttpMethod action : actions) {
            if (action == HttpMethod.GET) {
                builder.GET(route.getPath(), func);
            }
            else if (action == HttpMethod.POST) {
                builder.POST(route.getPath(), func);
            }
        }
    }

    private GatewayServiceRegistry.ServiceMethod getServiceMethod(Route route,
            GatewayServiceRegistry.ServiceInfo serviceInfo) {
        val method = route.getMethod();
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(serviceInfo);
        val serviceMethod = methods.get(method);
        if (serviceMethod == null) {
            throw new NullPointerException(String.format("method of %s is null for not registered", route));
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
