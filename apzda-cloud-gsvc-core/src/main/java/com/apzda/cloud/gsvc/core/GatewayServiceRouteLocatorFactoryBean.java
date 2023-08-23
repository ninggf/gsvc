package com.apzda.cloud.gsvc.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

/**
 * 把服务注册到网关.
 *
 * @author ninggf
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayServiceRouteLocatorFactoryBean implements FactoryBean<RouteDefinitionLocator>, ApplicationContextAware {
    private final String serviceId;
    private final String appName;
    /**
     * -1 表示当前微服务
     */
    private final int index;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RouteDefinitionLocator getObject() throws Exception {
        return new GatewayServiceRouteDefinitionLocator(appName, serviceId, index, applicationContext);
    }

    @Override
    public Class<?> getObjectType() {
        return RouteDefinitionLocator.class;
    }
}
