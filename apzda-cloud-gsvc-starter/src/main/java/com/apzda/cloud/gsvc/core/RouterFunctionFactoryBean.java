package com.apzda.cloud.gsvc.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

/**
 * @author ninggf
 */
@Slf4j
@RequiredArgsConstructor
public class RouterFunctionFactoryBean implements FactoryBean<RouterFunction<ServerResponse>>, ApplicationContextAware {

    private final String appName;

    private final String serviceName;

    private final Class<?> clazz;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RouterFunction<ServerResponse> getObject() {
        return createRouterFunction(appName, serviceName, clazz, applicationContext);
    }

    @Override
    public Class<?> getObjectType() {
        return RouterFunction.class;
    }

    public static RouterFunction<ServerResponse> createRouterFunction(String app, String service, Class<?> clazz,
            ApplicationContext applicationContext) {
        val methods = GatewayServiceRegistry.getServiceMethods(app, service, clazz);

        val route = RouterFunctions.route();
        var contextPath = applicationContext.getEnvironment().getProperty("server.servlet.context-path");
        if (StringUtils.isBlank(contextPath)) {
            contextPath = "/" + app;
        }
        else {
            contextPath = "";
        }

        for (Map.Entry<String, GatewayServiceRegistry.MethodInfo> method : methods.entrySet()) {
            val methodName = method.getKey();
            val methodHolder = method.getValue();
            val path = "/" + service + "/" + methodName;

            log.trace("Route {} to {}@{}", path, service, methodName);
            route.POST(contextPath + path,
                    request -> ServiceMethodHandler.handle(request, methodHolder, applicationContext));
        }

        return route.build();
    }

}
