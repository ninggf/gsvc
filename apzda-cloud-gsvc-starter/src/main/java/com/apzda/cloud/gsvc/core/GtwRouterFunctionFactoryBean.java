package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import com.apzda.cloud.gsvc.gtw.Route;
import com.apzda.cloud.gsvc.server.ServiceMethodHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.*;

import java.util.List;

import static org.springdoc.core.utils.Constants.OPERATION_ATTRIBUTE;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class GtwRouterFunctionFactoryBean
        implements FactoryBean<RouterFunction<ServerResponse>>, ApplicationContextAware {

    private final Route route;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RouterFunction<ServerResponse> getObject() throws Exception {
        val router = RouterFunctions.route();
        val interfaceName = route.getInterfaceName();
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(interfaceName);
        val svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        val globalFilters = svcConfigure.getGlobalFilters();
        val serviceBean = applicationContext.getBean(interfaceName);

        setupRoute(router, route, serviceInfo);
        setupFilter(route, router, globalFilters);

        val exceptionHandler = applicationContext.getBean(GsvcExceptionHandler.class);
        // bookmark exception handle(gtw call)
        return router.onError(Exception.class, exceptionHandler::handle).build();
    }

    private void setupRoute(RouterFunctions.Builder builder, Route route, ServiceInfo serviceInfo) {
        val actions = route.getActions();
        val serviceMethod = getServiceMethod(route, serviceInfo);
        val path = route.absPath();
        val meta = route.meta();

        if (log.isDebugEnabled()) {
            log.debug("SN Route {} to {}.{}({})", path, serviceMethod.getServiceName(), serviceMethod.getDmName(),
                    meta);
        }

        if (meta.isLogin()) {
            GatewayServiceRegistry.registerRouteMeta(path, meta);
        }

        final HandlerFunction<ServerResponse> func = request -> ServiceMethodHandler.handle(request, "gtw",
                serviceMethod, applicationContext);

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

        log.warn("为{}生成文档!", path);
        val operationBuilder = createOperationBuilder(route, serviceMethod);
        operationBuilder.operationId(path);
        builder.withAttribute(OPERATION_ATTRIBUTE, operationBuilder);
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

    private Builder createOperationBuilder(Route route, ServiceMethod serviceMethod) {
        val ro = Builder.operationBuilder();
        ro.beanClass(serviceMethod.getInterfaceName()).beanMethod(serviceMethod.getDmName());
        ro.operationId(route.absPath());
        // TODO 完成swagger
        return ro;
    }

}
