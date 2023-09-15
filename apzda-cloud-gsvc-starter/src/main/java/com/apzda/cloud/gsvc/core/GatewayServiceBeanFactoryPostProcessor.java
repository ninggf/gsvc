package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.gtw.GroupRoute;
import com.apzda.cloud.gsvc.gtw.Route;
import com.google.protobuf.Descriptors;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
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
import org.springframework.web.reactive.function.client.WebClient;
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
            .map(n -> {
                try {
                    val beanDefinition = beanFactory.getBeanDefinition(n);
                    val beanClassName = Objects.requireNonNull(beanDefinition.getBeanClassName()).replace("Gsvc", "");
                    val aClass = Class.forName(beanClassName);
                    val grpcClazz = Class.forName(beanClassName + "Grpc");
                    val method = grpcClazz.getMethod("getServiceDescriptor");
                    GatewayServiceRegistry.register(aClass, (ServiceDescriptor) method.invoke(null));
                }
                catch (Exception e) {
                    throw new BeanCreationException(e.getMessage(), e);
                }

                return GatewayServiceRegistry.cfgName(n);
            })
            .toList();

        for (String cfgName : services) {
            val interfaceName = environment.getProperty("apzda.cloud.service." + cfgName + ".interface-name");

            if (StringUtils.isNotBlank(interfaceName)) {
                log.debug("Found Gsvc Service: {} - {}", cfgName, interfaceName);
                try {
                    val aClass = Class.forName(interfaceName);
                    if (bf.getBeanNamesForType(aClass).length == 0) {
                        throw new BeanDefinitionValidationException("No bean of '" + aClass + "' Found");
                    }

                    val serviceName = GatewayServiceRegistry.svcName(aClass);
                    log.info("Register Gsvc Service: {} - {}", serviceName, aClass);
                    // 注册服务
                    GatewayServiceRegistry.register(aClass);
                    // 注册服务路由
                    registerRouterFunction(bf, aClass);
                    // 注册网关路由
                    val prefix = "apzda.cloud.gateway." + cfgName + ".routes";
                    val routes = createRoutes(prefix, aClass, environment);
                    for (GroupRoute route : routes) {
                        registerRouterFunction(bf, route);
                    }
                    val descriptor = GatewayServiceRegistry.SERVCIE_DESCRIPTOR.get(aClass);

                    if (descriptor != null) {
                        val defaultFilters = environment.getProperty("apzda.cloud.gateway.default.filters");
                        List<GroupRoute> routes1 = new ArrayList<>();
                        for (MethodDescriptor<?, ?> method : descriptor.getMethods()) {
                            if (method.getSchemaDescriptor() instanceof ProtoMethodDescriptorSupplier ms) {
                                val route = createRoute(aClass, ms.getMethodDescriptor(), defaultFilters);
                                if (route != null) {
                                    routes1.add(route);
                                }
                            }
                        }

                        for (GroupRoute route : routes1) {
                            registerRouterFunction(bf, route);
                        }
                    }
                }
                catch (ClassNotFoundException e) {
                    throw new BeanCreationException(e.getMessage(), e);
                }
            }
            else {
                // 注册WebClient
                registerWebclient(cfgName, bf);
            }
        }
    }

    private void registerWebclient(String cfgName, DefaultListableBeanFactory bf) {
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(WebclientFactoryBean.class);

        definition.addConstructorArgValue(cfgName);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        String[] qualifiers = new String[] { cfgName + "Webclient" };
        val beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, WebClient.class.getName());

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, cfgName + "WebClient", qualifiers);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, bf);

        log.trace("Register WebClient: '{}WebClient'", cfgName);
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, Class<?> clazz) {
        val serviceName = GatewayServiceRegistry.svcName(clazz);
        val cfgName = GatewayServiceRegistry.cfgName(clazz);
        if (REGISTERED.getOrDefault(serviceName, false)) {
            return;
        }
        REGISTERED.put(serviceName, true);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RouterFunctionFactoryBean.class);

        definition.addConstructorArgValue(cfgName);
        definition.addConstructorArgValue(serviceName);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        BeanDefinitionHolder holder = getBeanDefinitionHolder(cfgName, serviceName, definition);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private List<GroupRoute> createRoutes(String prefix, Class<?> interfaceName, Environment environment)
            throws ClassNotFoundException {

        val groupRoutes = new ArrayList<GroupRoute>();

        for (int i = 0; i < 10000; i++) {
            if (interfaceName == null) {
                val clazz = environment.getProperty(prefix + "[" + i + "].interface-name");
                if (StringUtils.isBlank(clazz)) {
                    return groupRoutes;
                }
                interfaceName = Class.forName(clazz);
            }
            val route = createRoute(prefix, interfaceName, i, environment, null);
            if (route == null) {
                break;
            }

            log.debug("Found Route: {}[{}] -> {}", prefix, i, route);
            List<Route> subRoutes = new ArrayList<>();
            for (int j = 0; j < 10000; j++) {
                val subPrefix = prefix + "[" + i + "].routes";
                val subRoute = createRoute(subPrefix, interfaceName, j, environment, route);
                if (subRoute == null) {
                    break;
                }
                log.debug("Found Route: {}[{}] -> {}", subPrefix, j, subRoute);
                subRoutes.add(subRoute);
            }
            val groupRoute = GroupRoute.valueOf(route);
            groupRoute.setRoutes(subRoutes);
            groupRoutes.add(groupRoute);
        }
        return groupRoutes;
    }

    private Route createRoute(String prefix, Class<?> interfaceName, int index, Environment environment, Route parent) {
        prefix = prefix + "[" + index + "]";
        val path = environment.getProperty(prefix + ".path");
        if (StringUtils.isBlank(path)) {
            return null;
        }
        val login = environment.getProperty(prefix + ".login");
        val method = environment.getProperty(prefix + ".method");
        val actions = environment.getProperty(prefix + ".actions");
        val access = environment.getProperty(prefix + ".access");
        var filters = environment.getProperty(prefix + ".filters");

        if (parent == null) {
            val defaultFilters = environment.getProperty("apzda.cloud.gateway.default.filters");
            if (StringUtils.isNotBlank(defaultFilters)) {
                if (StringUtils.isBlank(filters)) {
                    filters = defaultFilters;
                }
                else {
                    filters = defaultFilters + "," + filters;
                }
            }
        }

        return new Route().prefix(prefix)
            .parent(parent)
            .index(index)
            .path(path)
            .interfaceName(interfaceName)
            .method(method)
            .actions(actions)
            .login(login)
            .access(access)
            .filters(filters);
    }

    private GroupRoute createRoute(Class<?> aClass, Descriptors.MethodDescriptor methodDescriptor,
            String defaultFilters) {
        val options = methodDescriptor.getOptions();
        var path = options.getExtension(GsvcExt.path).trim();
        if (StringUtils.isBlank(path)) {
            return null;
        }
        var index = methodDescriptor.getIndex() + 10001;
        path = "/" + StringUtils.strip(path, "/");
        val login = options.getExtension(GsvcExt.login);
        val access = options.getExtension(GsvcExt.access).trim();
        val methods = options.getExtension(GsvcExt.methods).trim();
        var filters = options.getExtension(GsvcExt.filters).trim();

        if (StringUtils.isNotBlank(defaultFilters)) {
            if (StringUtils.isBlank(filters)) {
                filters = defaultFilters;
            }
            else {
                filters = defaultFilters + "," + filters;
            }
        }

        val route = new Route();
        route.setPath(path);
        route.index(index);
        route.setInterfaceName(aClass);
        route.setMethod(methodDescriptor.getName());
        route.filters(filters);
        route.actions(StringUtils.defaultIfBlank(methods, "post"));
        route.setLogin(login);
        route.access(access);

        return GroupRoute.valueOf(route);
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, GroupRoute route) {
        // to
        val interfaceName = route.getInterfaceName();
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(interfaceName);
        if (serviceInfo == null) {
            log.warn("Service not found for route: {} ", route);
            return;
        }

        val cfgName = serviceInfo.getCfgName();
        val serviceName = serviceInfo.getServiceName();
        val definition = BeanDefinitionBuilder.genericBeanDefinition(GtwRouterFunctionFactoryBean.class);

        definition.addConstructorArgValue(route);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        val holder = getBeanDefinitionHolder(cfgName, serviceName + ".route." + route.index(), definition);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

    }

    private BeanDefinitionHolder getBeanDefinitionHolder(String appName, String serviceName,
            BeanDefinitionBuilder definition) {
        String[] qualifiers = new String[] { appName + "_" + serviceName };
        val beanDefinition = definition.getBeanDefinition();
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

}
