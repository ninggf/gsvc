package com.apzda.cloud.gsvc.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.UUID;


/**
 * @author ninggf
 */
@Slf4j
@RequiredArgsConstructor
public class RouterFunctionFactoryBean implements FactoryBean<RouterFunction<ServerResponse>>, ApplicationContextAware {
    private final String app;
    private final String name;
    private final Class<?> clazz;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RouterFunction<ServerResponse> getObject() throws Exception {
        return createRouterFunction(app, name, clazz, applicationContext);
    }

    @Override
    public Class<?> getObjectType() {
        return RouterFunction.class;
    }

    public static RouterFunction<ServerResponse> createRouterFunction(
        String app,
        String service,
        Class<?> clazz,
        ApplicationContext applicationContext
    ) {
        val methods = GatewayServiceRegistry.getServiceMethods(app, service, clazz);

        val route = RouterFunctions.route().before((serverRequest -> {
            val header = serverRequest.headers().firstHeader("X-Request-Id");
            if (!StringUtils.hasText(header)) {
                return ServerRequest.from(serverRequest).header("X-Request-Id", UUID.randomUUID().toString()).build();
            }
            return serverRequest;
        }));

        for (Map.Entry<String, GatewayServiceRegistry.MethodInfo> method : methods.entrySet()) {
            val key = method.getKey();
            val mInfo = method.getValue();
            val path = "/" + service + "/" + key;
            log.debug("Route {} to {}@{}", path, service, key);

            route.GET(path, request ->
                ServiceMethodHandler.handle(request, mInfo, applicationContext)
            ).POST(path, request ->
                ServiceMethodHandler.handle(request, mInfo, applicationContext));
        }

        return route.build();
    }
}
