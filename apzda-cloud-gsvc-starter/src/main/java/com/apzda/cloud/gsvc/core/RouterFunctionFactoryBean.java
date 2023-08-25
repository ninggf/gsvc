package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.SaManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
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
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
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

        val route = RouterFunctions.route();

        for (Map.Entry<String, GatewayServiceRegistry.MethodInfo> method : methods.entrySet()) {
            val methodName = method.getKey();
            val methodHolder = method.getValue();
            val path = "/" + service + "/" + methodName;
            log.trace("GroupRoute {} to {}@{}", path, service, methodName);
            route.POST(path, request -> ServiceMethodHandler.handle(request, methodHolder, applicationContext));
        }

        return route.build();
    }
}
