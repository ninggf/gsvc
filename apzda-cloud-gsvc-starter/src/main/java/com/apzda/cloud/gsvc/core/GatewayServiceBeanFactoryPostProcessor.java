package com.apzda.cloud.gsvc.core;

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
                        registerRouterFunction(bf, aClass, serviceName, app, i);
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
                // 忽略 apzda.cloud.service.app使用spring.application.name
                val app = environment.getProperty("spring.application.name");
                val name = getServiceName(environment.getProperty("apzda.cloud.service.name"), aClass);
                log.info("Registered Default Service: {}@{} - {}", app, name, aClass);
                registerRouterFunction(bf, aClass, name, app, -1);
            }
            catch (ClassNotFoundException e) {
                throw new BeanCreationException(interfaceName, e);
            }
        }

        val gtwEnabled = Boolean.parseBoolean(environment.getProperty("apzda.cloud.gtw-enabled"));

        if (gtwEnabled) {
            createRoutes(bf, environment);
        }
    }

    private void createRoutes(BeanDefinitionRegistry registry, Environment environment) {
        log.trace("start to register routes");

        for (int i = 0; i < 1000; i++) {
            val route = createRoute("apzda.cloud.routes", i, environment, null);
            if (route == null) {
                break;
            }
            registerRouterFunction(registry, route);
            for (int j = 0; j < 10000; j++) {
                val subRoute = createRoute("apzda.cloud.routes[" + i + "].routes", j, environment, route);
                if (subRoute == null) {
                    break;
                }
                registerRouterFunction(registry, subRoute);
            }
        }
    }

    private void registerServiceProxy(BeanDefinitionRegistry registry, String className,
            Map<String, Object> attributes) {
        String name = (String) attributes.get("name");
        String app = (String) attributes.get("app");
        if (registered.getOrDefault(app + ":" + name, false)) {
            return;
        }
        registered.put(app + ":" + name, true);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder
            .genericBeanDefinition(ReferenceServiceFactoryBean.class);

        definition.addConstructorArgValue(name);
        definition.addConstructorArgValue(app);
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
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, app + name + "Impl", qualifiers);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, Class<?> clazz, String serviceName,
            String appName, int index) {
        if (registered.getOrDefault(appName + "@" + serviceName, false)) {
            return;
        }
        registered.put(appName + "@" + serviceName, true);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RouterFunctionFactoryBean.class);

        definition.addConstructorArgValue(appName);
        definition.addConstructorArgValue(serviceName);
        definition.addConstructorArgValue(clazz);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        String[] qualifiers = new String[] { appName + "_" + serviceName };
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, RouterFunction.class.getName());
        boolean primary = true;
        beanDefinition.setPrimary(primary);

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition,
                appName + serviceName + ".RouterFunctionFactoryBean", qualifiers);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

        GatewayServiceRegistry.markLocalService(appName, serviceName, index);
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, Route route) {
        // to
    }

    private Route createRoute(String prefix, int index, Environment environment, Route parent) {
        prefix = prefix + "[" + index + "].";
        val path = environment.getProperty(prefix + "path");
        if (StringUtils.isBlank(path)) {
            return null;
        }
        val interfaceName = environment.getProperty(prefix + "interface-name");
        val login = environment.getProperty("login");
        val method = environment.getProperty(prefix + "method");
        val filters = environment.getProperty(prefix + "filters");
        return new Route().parent(parent)
            .path(path)
            .interfaceName(interfaceName)
            .method(method)
            .login(login)
            .filters(filters);
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
