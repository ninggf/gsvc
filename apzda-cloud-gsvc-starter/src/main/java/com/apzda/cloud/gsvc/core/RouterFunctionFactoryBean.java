package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.server.ServiceMethodHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

    private final String cfgName;

    private final String serviceName;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RouterFunction<ServerResponse> getObject() {
        return createRouterFunction(cfgName, serviceName, applicationContext);
    }

    @Override
    public Class<?> getObjectType() {
        return RouterFunction.class;
    }

    public static RouterFunction<ServerResponse> createRouterFunction(String cfgName, String serviceName,
            ApplicationContext applicationContext) {
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(cfgName, serviceName);

        val route = RouterFunctions.route();
        for (Map.Entry<String, ServiceMethod> method : methods.entrySet()) {
            val methodName = method.getKey();
            val methodHolder = method.getValue();
            val path = "/~" + serviceName + "/" + methodName;

            log.debug("EW Route {} to {}.{}", path, serviceName, methodName);
            route.POST(path, request -> ServiceMethodHandler.handle(request, null, methodHolder, applicationContext));
        }
        val errorHandler = applicationContext.getBean(GsvcExceptionHandler.class);

        // bookmark exception handle(service call)
        return route.onError(Exception.class, errorHandler::handle).build();
    }

}
