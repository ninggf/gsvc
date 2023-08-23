package com.apzda.cloud.gsvc.core;

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
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

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
    private final Map<String, Boolean> registered = new HashMap<>();

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory bf = (DefaultListableBeanFactory) beanFactory;
        Environment environment = (Environment) bf.getBean("environment");
        String interfaceName;
        for (int i = 0; i < 10000; ++i) {
            interfaceName = environment.getProperty("apzda.cloud.reference[" + i + "].interface-name");
            if (interfaceName != null) {
                try {
                    val aClass = Class.forName(interfaceName);

                    val id = getServiceName(environment.getProperty("apzda.cloud.reference[" + i + "].name"), aClass);
                    val app = StringUtils.defaultIfBlank(environment.getProperty(
                        "apzda.cloud.reference[" + i + "].app"), id);

                    log.info("Registered Reference Service: {}@{} - {}", app, id, aClass);
                    if (bf.getBeanNamesForType(aClass).length > 0) {
                        // 服务在本地时定义服务访问路由, app使用spring.application.name值
                        registerRouteDefinitionLocator(bf, id, environment.getProperty("spring.application.name"), i);
                    } else if (aClass.isInterface()) {
                        val attributes = new HashMap<String, Object>();
                        attributes.put("interfaceClass", aClass);
                        attributes.put("className", interfaceName);
                        attributes.put("app", app);
                        attributes.put("name", id);
                        attributes.put("primary", true);
                        attributes.put("qualifiers", new String[]{id + "Impl"});
                        attributes.put("index", i);
                        registerServiceProxy(bf, interfaceName, attributes);
                    }
                } catch (ClassNotFoundException e) {
                    throw new BeanCreationException(interfaceName, e);
                }
            } else {
                break;
            }
        }

        interfaceName = environment.getProperty("apzda.cloud.service.interface-name");
        if (org.apache.commons.lang3.StringUtils.isNotBlank(interfaceName)) {
            try {
                val aClass = Class.forName(interfaceName);
                if (bf.getBeanNamesForType(aClass).length == 0) {
                    throw new BeanDefinitionValidationException("No bean of '" + aClass + "' Found");
                }
                // 本忽略 apzda.cloud.service.app使用spring.application.name
                val app = environment.getProperty("spring.application.name");
                val name = getServiceName(environment.getProperty("apzda.cloud.service.name"), aClass);
                log.info("Registered Default Service: {}@{} - {}", app, name, aClass);
                registerRouteDefinitionLocator(bf, name, app, -1);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(interfaceName, e);
            }
        }
    }

    private void registerServiceProxy(BeanDefinitionRegistry registry,
                                      String className,
                                      Map<String, Object> attributes) {

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(ReferenceServiceFactoryBean.class);
        String name = (String) attributes.get("name");
        definition.addConstructorArgValue(name);
        definition.addConstructorArgValue(attributes.get("app"));
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
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, qualifiers);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private void registerRouteDefinitionLocator(BeanDefinitionRegistry registry, String name, String app, int index) {
        val routeId = app + name;
        if (registered.getOrDefault(routeId, false)) {
            return;
        }
        registered.put(routeId, true);
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(
            GatewayServiceRouteLocatorFactoryBean.class);
        log.info("Registered RouteDefinitionLocator for {}@{}", name, app);
        definition.addConstructorArgValue(name);
        definition.addConstructorArgValue(app);
        definition.addConstructorArgValue(index);
        val beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, RouteDefinitionLocator.class);
        registry.registerBeanDefinition(name + ".RouteDefinitionLocator", beanDefinition);
        GatewayServiceRegistry.markLocalService(app, name, index);
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
        qualifierList.removeIf(qualifier -> StringUtils.isBlank(qualifier));
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
