package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.gtw.GroupRoute;
import com.apzda.cloud.gsvc.gtw.Route;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.function.RouterFunction;

import java.util.*;

/**
 * 服务配置处理器，将引用的类配置到容器并时定义服务访问路由. <br/>
 * 1. 引用服务通过"apzda.cloud.reference"配置.<br/>
 * 2. 本地服务通过"apzda.cloud.service"配置
 *
 * @author fengz
 */
@Slf4j
public class GatewayServiceBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final Map<String, Boolean> REGISTERED = new HashMap<>();

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory bf = (DefaultListableBeanFactory) beanFactory;
        Environment environment = (Environment) bf.getBean("environment");

        val services = Arrays.stream(beanFactory.getBeanDefinitionNames())
            .filter(n -> StringUtils.endsWith(n, "Gsvc"))
            .map(GatewayServiceRegistry::shortSvcName)
            .toList();
        environment.getProperty("abc.abc.f","f");
        // 注册本地服务
        for (String appName : services) {
            val interfaceName = environment.getProperty("apzda.cloud.service." + appName + ".interface-name");
            log.debug("Found Gsvc Service: {} - {}", appName, interfaceName);

            if (StringUtils.isNotBlank(interfaceName)) {
                try {
                    val aClass = Class.forName(interfaceName);
                    if (bf.getBeanNamesForType(aClass).length == 0) {
                        throw new BeanDefinitionValidationException("No bean of '" + aClass + "' Found");
                    }

                    val serviceName = GatewayServiceRegistry.svcName(aClass);
                    log.info("Register Gsvc Service: {} - {}", serviceName, aClass);
                    // 注册服务
                    GatewayServiceRegistry.register(aClass);
                    // 注册东西路由
                    registerRouterFunction(bf, aClass);
                    // 注册南北路由
                    createRoutes(appName, aClass, bf, environment);
                }
                catch (ClassNotFoundException e) {
                    throw new BeanCreationException(interfaceName, e);
                }
            }
        }
    }

    private void createRoutes(String app, Class<?> interfaceName, BeanDefinitionRegistry registry,
            Environment environment) throws ClassNotFoundException {
        if (log.isDebugEnabled()) {
            log.debug("Register Routes: {} - {}", GatewayServiceRegistry.svcName(interfaceName), interfaceName);
        }
        for (int i = 0; i < 10000; i++) {
            val route = createRoute(app, interfaceName, "apzda.cloud.service." + app + ".routes", i, environment, null);
            if (route == null) {
                break;
            }
            log.debug("Found Route: apzda.cloud.service.{}.routes[{}] -> {}", app, i, route);
            List<Route> subRoutes = new ArrayList<>();
            for (int j = 0; j < 10000; j++) {
                val subRoute = createRoute(app, interfaceName,
                        "apzda.cloud.service." + app + ".routes[" + i + "].routes", j, environment, route);
                if (subRoute == null) {
                    break;
                }
                log.debug("Found Route: apzda.cloud.service.{}.routes[{}].routes[{}] -> {}", app, i, j, subRoute);
                subRoutes.add(subRoute);
            }
            val groupRoute = GroupRoute.valueOf(route);
            groupRoute.setRoutes(subRoutes);
            registerRouterFunction(registry, groupRoute);
        }
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, Class<?> clazz) {
        val serviceName = GatewayServiceRegistry.svcName(clazz);
        val svcName = GatewayServiceRegistry.shortSvcName(clazz);
        if (REGISTERED.getOrDefault(serviceName, false)) {
            return;
        }
        REGISTERED.put(serviceName, true);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RouterFunctionFactoryBean.class);

        definition.addConstructorArgValue(svcName);
        definition.addConstructorArgValue(serviceName);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        BeanDefinitionHolder holder = getBeanDefinitionHolder(svcName, serviceName, definition);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private Route createRoute(String app, Class<?> interfaceName, String prefix, int index, Environment environment,
            Route parent) {
        prefix = prefix + "[" + index + "]";
        val path = environment.getProperty(prefix + ".path");
        if (StringUtils.isBlank(path)) {
            return null;
        }
        val login = environment.getProperty(prefix + ".login");
        val method = environment.getProperty(prefix + ".method");
        val actions = environment.getProperty(prefix + ".actions");
        val filters = environment.getProperty(prefix + ".filters");

        return new Route().app(app)
            .parent(parent)
            .index(index)
            .path(path)
            .interfaceName(interfaceName)
            .method(method)
            .actions(actions)
            .login(login)
            .filters(filters);
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, GroupRoute route) {
        // to
        val interfaceName = route.getInterfaceName();
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(interfaceName);
        if (serviceInfo == null) {
            log.warn("Service not found for route: {} ", route);
            return;
        }

        val appName = serviceInfo.getAppName();
        val serviceName = serviceInfo.getServiceName();
        BeanDefinitionBuilder definition = BeanDefinitionBuilder
            .genericBeanDefinition(GroupRouterFunctionFactoryBean.class);

        definition.addConstructorArgValue(route);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        BeanDefinitionHolder holder = getBeanDefinitionHolder(appName, serviceName + ".route." + route.index(),
                definition);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

    }

    private BeanDefinitionHolder getBeanDefinitionHolder(String appName, String serviceName,
            BeanDefinitionBuilder definition) {
        String[] qualifiers = new String[] { appName + "_" + serviceName };
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, RouterFunction.class.getName());
        boolean primary = true;
        beanDefinition.setPrimary(primary);

        return new BeanDefinitionHolder(beanDefinition, appName + "." + serviceName + ".RouterFunctionFactoryBean",
                qualifiers);
    }

    private String getQualifier(Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String qualifier = (String) client.get("qualifier");
        if (StringUtils.isNotBlank(qualifier)) {
            return qualifier;
        }
        return null;
    }

    private String[] getQualifiers(Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        List<String> qualifierList = new ArrayList<>(Arrays.asList((String[]) client.get("qualifiers")));
        qualifierList.removeIf(StringUtils::isBlank);
        if (qualifierList.isEmpty() && getQualifier(client) != null) {
            qualifierList = Collections.singletonList(getQualifier(client));
        }
        return !qualifierList.isEmpty() ? qualifierList.toArray(new String[0]) : null;
    }

    private String getServiceName(String name, Class<?> interfaceName) {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        val canonicalName = interfaceName.getSimpleName();
        return canonicalName.substring(0, 1).toLowerCase() + canonicalName.substring(1);
    }

}
