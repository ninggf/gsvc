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

    private static final Map<String, Boolean> registered = new HashMap<>();

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory bf = (DefaultListableBeanFactory) beanFactory;
        Environment environment = (Environment) bf.getBean("environment");
        String interfaceName;

        // 注册远程服务
        for (int i = 0; i < 10000; ++i) {
            interfaceName = environment.getProperty("apzda.cloud.reference[" + i + "].interface-name");
            if (interfaceName != null) {
                try {
                    val aClass = Class.forName(interfaceName);
                    val serviceName = getServiceName(environment.getProperty("apzda.cloud.reference[" + i + "].name"),
                            aClass);
                    if (bf.getBeanNamesForType(aClass).length > 0) {
                        // 服务在本地时定义服务访问路由, app使用spring.application.name值
                        val app = environment.getProperty("spring.application.name");
                        val contextPath = environment.getProperty("server.servlet.context-path");
                        registerRouterFunction(bf, aClass, serviceName, app, contextPath, i);
                        log.info("Registered Reference Service: {}@{} - {}", app, serviceName, aClass);
                    }
                    else if (aClass.isInterface()) {
                        val app = StringUtils.defaultIfBlank(
                                environment.getProperty("apzda.cloud.reference[" + i + "].app"), serviceName);
                        val attributes = new HashMap<String, Object>();
                        val contextPath = environment.getProperty("apzda.cloud.reference[" + i + "].context-path");
                        attributes.put("interfaceClass", aClass);
                        attributes.put("className", interfaceName);
                        attributes.put("app", app);
                        attributes.put("name", serviceName);
                        attributes.put("contextPath", contextPath);
                        attributes.put("primary", true);
                        attributes.put("qualifiers", new String[] { app + "@" + serviceName + "Impl" });
                        attributes.put("index", i);
                        registerServiceProxy(bf, interfaceName, attributes);
                        log.info("Registered Reference Service: {}@{} - {}", app, serviceName, aClass);
                    }
                }
                catch (ClassNotFoundException e) {
                    throw new BeanCreationException(interfaceName, e);
                }
            }
            else {
                break;
            }
        }

        // 注册本地服务
        interfaceName = environment.getProperty("apzda.cloud.service.interface-name");

        if (StringUtils.isNotBlank(interfaceName)) {
            try {
                val aClass = Class.forName(interfaceName);
                if (bf.getBeanNamesForType(aClass).length == 0) {
                    throw new BeanDefinitionValidationException("No bean of '" + aClass + "' Found");
                }
                // 忽略 apzda.cloud.service.app使用spring.application.serviceName
                val appName = environment.getProperty("spring.application.name");
                val contextPath = environment.getProperty("server.servlet.context-path");
                val serviceName = getServiceName(environment.getProperty("apzda.cloud.service.serviceName"), aClass);
                log.info("Registered Default Service: {}@{} - {}", appName, serviceName, aClass);
                registerRouterFunction(bf, aClass, serviceName, appName, contextPath, -1);
            }
            catch (ClassNotFoundException e) {
                throw new BeanCreationException(interfaceName, e);
            }
        }

        // 注册路由
        val gtwEnabled = Boolean.parseBoolean(environment.getProperty("apzda.cloud.gtw-enabled"));

        if (gtwEnabled) {
            try {
                createRoutes(bf, environment);
            }
            catch (Exception e) {
                throw new BeanCreationException(e.getMessage(), e);
            }
        }
    }

    private void createRoutes(BeanDefinitionRegistry registry, Environment environment) throws ClassNotFoundException {
        log.trace("Start to register routes");

        for (int i = 0; i < 1000; i++) {
            val route = createRoute("apzda.cloud.routes", i, environment, null);
            if (route == null) {
                break;
            }
            log.debug("Found Route: {}[{}]", "apzda.cloud.routes", i);
            List<Route> subRoutes = new ArrayList<>();
            for (int j = 0; j < 10000; j++) {
                val subRoute = createRoute("apzda.cloud.routes[" + i + "].routes", j, environment, route);
                if (subRoute == null) {
                    break;
                }
                log.debug("Found Route: {}[{}].routes[{}]", "apzda.cloud.routes", i, j);
                subRoutes.add(subRoute);
            }
            val groupRoute = new GroupRoute(route);
            groupRoute.setRoutes(subRoutes);
            registerRouterFunction(registry, groupRoute);
        }
    }

    private void registerServiceProxy(BeanDefinitionRegistry registry, String className,
            Map<String, Object> attributes) {
        String serviceName = (String) attributes.get("serviceName");
        String appName = (String) attributes.get("appName");
        if (registered.getOrDefault(appName + ":" + serviceName, false)) {
            return;
        }
        registered.put(appName + ":" + serviceName, true);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder
            .genericBeanDefinition(ReferenceServiceFactoryBean.class);

        definition.addConstructorArgValue(serviceName);
        definition.addConstructorArgValue(appName);
        definition.addConstructorArgValue(attributes.get("contextPath"));
        definition.addConstructorArgValue(className);
        definition.addConstructorArgValue(attributes.get("index"));
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        String[] qualifiers = getQualifiers(attributes);
        // This is done so that there's a way to retrieve qualifiers while generating AOT
        // code
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
        // has a default, won't be null
        boolean primary = (Boolean) attributes.get("primary");
        beanDefinition.setPrimary(primary);
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, appName + serviceName + "Impl",
                qualifiers);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

        val serviceInfo = GatewayServiceRegistry.ServiceInfo.builder()
            .appName(appName)
            .serviceName(serviceName)
            .contextPath((String) attributes.get("contextPath"))
            .clazz((Class<?>) attributes.get("interfaceClass"))
            .index((int) attributes.get("index"))
            .build();

        GatewayServiceRegistry.registerServiceInfo(serviceInfo);
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, Class<?> clazz, String serviceName,
            String appName, String contextPath, int index) {
        if (registered.getOrDefault(appName + "@" + serviceName, false)) {
            return;
        }
        registered.put(appName + "@" + serviceName, true);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RouterFunctionFactoryBean.class);

        definition.addConstructorArgValue(appName);
        definition.addConstructorArgValue(serviceName);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        BeanDefinitionHolder holder = getBeanDefinitionHolder(appName, serviceName, definition);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        val serviceInfo = GatewayServiceRegistry.ServiceInfo.builder()
            .appName(appName)
            .serviceName(serviceName)
            .contextPath(contextPath)
            .clazz(clazz)
            .index(index)
            .local(true)
            .build();
        GatewayServiceRegistry.registerServiceInfo(serviceInfo);
    }

    private Route createRoute(String prefix, int index, Environment environment, Route parent) {
        prefix = prefix + "[" + index + "]";
        val path = environment.getProperty(prefix + ".path");
        if (StringUtils.isBlank(path)) {
            return null;
        }
        val serviceIndex = environment.getProperty(prefix + ".service-index");
        val login = environment.getProperty(prefix + ".login");
        val method = environment.getProperty(prefix + ".method");
        val actions = environment.getProperty(prefix + ".actions", "GET,POST");
        val filters = environment.getProperty(prefix + ".filters");
        try {
            return new Route().parent(parent)
                .index(index)
                .path(path)
                .serviceIndex(serviceIndex)
                .method(method)
                .actions(actions)
                .login(login)
                .filters(filters);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot parse '" + prefix + "' route", e);
        }
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, GroupRoute route) {
        // to
        val serviceIndex = route.getServiceIndex();
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(serviceIndex);
        if (serviceIndex == null) {
            log.debug("Service not found for route: {} ", route);
            return;
        }
        val localService = serviceInfo.isLocal();
        if (localService) {
            val appName = serviceInfo.getAppName();
            val serviceName = serviceInfo.getServiceName();
            BeanDefinitionBuilder definition = BeanDefinitionBuilder
                .genericBeanDefinition(GroupRoterFunctionFactoryBean.class);

            definition.addConstructorArgValue(route);
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            BeanDefinitionHolder holder = getBeanDefinitionHolder(appName, serviceName + ".route." + route.index(),
                    definition);

            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        }
        else {

        }
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
