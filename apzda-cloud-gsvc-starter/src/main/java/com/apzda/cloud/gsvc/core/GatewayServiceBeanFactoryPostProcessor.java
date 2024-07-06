package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.gtw.GroupRoute;
import com.apzda.cloud.gsvc.gtw.Route;
import com.google.api.AnnotationsProto;
import com.google.protobuf.Descriptors;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.RouterFunction;
import reactor.util.function.Tuples;

import java.time.Duration;
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

    private static final Logger webLog = LoggerFactory.getLogger("org.springframework.web");

    private String defaultRouteGlobalFilters;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory bf = (DefaultListableBeanFactory) beanFactory;
        Environment environment = (Environment) bf.getBean("environment");
        GsvcContextHolder.setAppName(environment.getProperty("spring.application.name"));

        val services = Arrays.stream(beanFactory.getBeanDefinitionNames())
            .filter(n -> StringUtils.endsWith(n, "Gsvc"))
            .map(n -> {
                try {
                    val beanDefinition = beanFactory.getBeanDefinition(n);
                    val beanClassName = Objects.requireNonNull(beanDefinition.getBeanClassName()).replace("Gsvc", "");
                    val aClass = Class.forName(beanClassName);
                    val grpcClazz = Class.forName(beanClassName + "Grpc");
                    val method = grpcClazz.getMethod("getServiceDescriptor");
                    val descriptor = (ServiceDescriptor) Objects.requireNonNull(method).invoke(null);
                    GatewayServiceRegistry.register(aClass, descriptor);

                    return Tuples.of(GatewayServiceRegistry.cfgName(n), aClass);
                }
                catch (NoSuchBeanDefinitionException | NullPointerException | ClassNotFoundException e) {
                    log.debug("{} is not a Gsvc Service since '{}', skip it.", n, e.getMessage());
                    return null;
                }
                catch (Exception e) {
                    throw new BeanCreationException(e.getMessage(), e);
                }
            })
            .filter(Objects::nonNull)
            .toList();

        // 默认网关全部过滤器
        this.defaultRouteGlobalFilters = environment.getProperty("apzda.cloud.gateway.default.filters");
        // 默认网关过滤器
        final boolean defaultGatewayEnabled = environment.getProperty("apzda.cloud.gateway.default.enabled",
                Boolean.class, false);

        for (val service : services) {
            val cfgName = service.getT1();
            val dashCfgName = com.apzda.cloud.gsvc.utils.StringUtils.toDashed(cfgName);
            val svcClz = service.getT2();
            val beanNames = beanFactory.getBeanNamesForType(svcClz, false, false);
            val isStubBean = Arrays.stream(beanNames)
                .anyMatch((name) -> StringUtils.startsWithAny(name, "gsvc", "grpc")
                        && StringUtils.endsWith(name, "Stub"));

            boolean gatewayEnabled;
            if (!isStubBean) {
                log.info("Found Gsvc Service(impl): {} - {}", cfgName, svcClz);
                // 注册服务路由
                // registerRouterFunction(bf, svcClz);
                gatewayEnabled = true;
            }
            else {
                val gsvcStub = "gsvc" + cfgName + "Stub";
                val grpcStub = "grpc" + cfgName + "Stub";

                if (environment.containsProperty("apzda.cloud.gateway." + dashCfgName + ".enabled")) {
                    gatewayEnabled = environment.getProperty("apzda.cloud.gateway." + dashCfgName + ".enabled",
                            Boolean.class, true);
                }
                else {
                    gatewayEnabled = defaultGatewayEnabled;
                }

                if (beanFactory.containsBeanDefinition(gsvcStub)) {
                    beanFactory.getBeanDefinition(gsvcStub);
                    log.info("Found Gsvc Service(http): {} - {}", cfgName, svcClz);
                    registerWebclient(cfgName, bf);
                }
                else if (beanFactory.containsBeanDefinition(grpcStub)) {
                    beanFactory.getBeanDefinition(grpcStub);
                    log.info("Found Gsvc Service(grpc): {} - {}", cfgName, svcClz);
                }
                else {
                    log.info("Found Gsvc Service(impl): {} - {}", cfgName, svcClz);
                    log.warn("No routes exported, only accessed locally: {} - {}", cfgName, svcClz);
                }
            }

            if (!gatewayEnabled) {
                continue;
            }

            try {
                // 注册网关路由(配置方式)
                val cfgPrefix = "apzda.cloud.gateway." + dashCfgName + ".routes";
                val prefix = environment.getProperty("apzda.cloud.gateway." + dashCfgName + ".prefix");
                val routes = createRoutes(cfgPrefix, environment);

                for (val route : routes) {
                    route.contextPath(prefix);
                    registerRouterFunction(bf, svcClz, route);
                }
                // 注册网关路由(定义方式)
                val descriptor = GatewayServiceRegistry.SERVICE_DESCRIPTOR.get(svcClz);
                if (descriptor != null) {
                    routes.clear();
                    for (MethodDescriptor<?, ?> method : descriptor.getMethods()) {
                        if (method.getSchemaDescriptor() instanceof ProtoMethodDescriptorSupplier ms) {
                            val route = createRoute(ms.getMethodDescriptor(), defaultRouteGlobalFilters);
                            if (route != null) {
                                routes.add(route);
                            }
                        }
                    }

                    for (val route : routes) {
                        route.contextPath(prefix);
                        registerRouterFunction(bf, svcClz, route);
                    }
                }
            }
            catch (ClassNotFoundException e) {
                throw new BeanCreationException(e.getMessage(), e);
            }
        }
    }

    private void registerWebclient(String cfgName, DefaultListableBeanFactory bf) {
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(WebclientFactoryBean.class);

        definition.addConstructorArgValue(cfgName);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        String[] qualifiers = new String[] { cfgName + "Webclient" };
        val beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, WebClient.class);

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, cfgName + "WebClient", qualifiers);

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, bf);

        log.trace("Registered {}WebClient Definition For: {}", cfgName, cfgName);
    }

    @Deprecated
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

    private List<Route> createRoutes(String prefix, Environment environment) throws ClassNotFoundException {

        val groupRoutes = new ArrayList<Route>();

        for (int i = 0; i < 10000; i++) {
            val route = createRoute(prefix, i, environment, null);
            if (route == null) {
                break;
            }

            webLog.debug("Found Route: {}[{}] -> {}", prefix, i, route);
            List<Route> subRoutes = new ArrayList<>();
            for (int j = 0; j < 10000; j++) {
                val subPrefix = prefix + "[" + i + "].routes";
                val subRoute = createRoute(subPrefix, j, environment, route);
                if (subRoute == null) {
                    break;
                }
                webLog.debug("Found Route: {}[{}] -> {}", subPrefix, j, subRoute);
                subRoutes.add(subRoute);
            }
            val groupRoute = GroupRoute.valueOf(route);
            groupRoute.setRoutes(subRoutes);
            groupRoutes.add(groupRoute);
        }
        return groupRoutes;
    }

    @Nullable
    private Route createRoute(String prefix, int index, @Nonnull Environment environment, Route parent) {
        prefix = prefix + "[" + index + "]";
        val path = environment.getProperty(prefix + ".path");
        if (StringUtils.isBlank(path)) {
            return null;
        }
        val login = environment.getProperty(prefix + ".login");
        val method = environment.getProperty(prefix + ".method");
        val actions = environment.getProperty(prefix + ".actions");
        val access = environment.getProperty(prefix + ".access");
        val summary = environment.getProperty(prefix + ".summary");
        val desc = environment.getProperty(prefix + ".desc");
        val tags = environment.getProperty(prefix + ".tags");
        val consumes = environment.getProperty(prefix + ".consumes");
        val readTimeout = environment.getProperty(prefix + ".read-timeout", Duration.class, Duration.ZERO);
        var filters = environment.getProperty(prefix + ".filters");

        if (parent == null) {
            if (StringUtils.isNotBlank(defaultRouteGlobalFilters)) {
                if (StringUtils.isBlank(filters)) {
                    filters = defaultRouteGlobalFilters;
                }
                else {
                    filters = defaultRouteGlobalFilters + "," + filters;
                }
            }
        }

        return new Route().prefix(prefix)
            .parent(parent)
            .index(index)
            .path(path)
            .method(method)
            .actions(actions)
            .login(login)
            .access(access)
            .readTimeout(readTimeout)
            .summary(summary)
            .tags(tags)
            .consumes(consumes)
            .desc(desc)
            .filters(filters);
    }

    @Nullable
    private Route createRoute(@Nonnull Descriptors.MethodDescriptor methodDescriptor, String defaultFilters) {
        val options = methodDescriptor.getOptions();
        val api = options.getExtension(GsvcExt.route);
        var path = api.getPath().trim();
        var methods = api.getMethods().trim();
        var consumes = api.getConsumes().trim();
        if (StringUtils.isBlank(path)) {
            val http = options.getExtension(AnnotationsProto.http);
            val number = http.getPatternCase().getNumber();
            switch (number) {
                case 2:
                    path = http.getGet().trim();
                    methods = "get";
                    break;
                case 3:
                    path = http.getPut().trim();
                    methods = "put";
                    break;
                case 4:
                    path = http.getPost().trim();
                    methods = "post";
                    break;
                case 5:
                    path = http.getDelete().trim();
                    methods = "delete";
                    break;
                case 6:
                    path = http.getPatch().trim();
                    methods = "patch";
                    break;
                default:
                    return null;
            }
            consumes = http.getBody();
            if (StringUtils.isBlank(path)) {
                return null;
            }
        }
        var index = methodDescriptor.getIndex() + 10001;
        path = "/" + StringUtils.strip(path, "/");
        val login = api.getLogin();
        val access = api.getAccess().trim();
        var filters = api.getFilters().trim();

        if (StringUtils.isNotBlank(defaultFilters)) {
            if (StringUtils.isBlank(filters)) {
                filters = defaultFilters;
            }
            else {
                filters = defaultFilters + "," + filters;
            }
        }

        val route = new Route();
        route.path(path);
        route.index(index);
        route.setSummary(api.getSummary());
        route.setDesc(api.getDesc());
        route.tags(api.getTags());
        route.setMethod(methodDescriptor.getName());
        route.filters(filters);
        route.actions(StringUtils.defaultIfBlank(methods, "post"));
        route.setLogin(login);
        route.consumes(consumes);
        route.access(access);
        if (api.getTimeout() > 0) {
            route.readTimeout(Duration.ofMillis(api.getTimeout()));
        }
        return GroupRoute.valueOf(route);
    }

    private void registerRouterFunction(BeanDefinitionRegistry registry, Class<?> serviceInterface, Route route) {
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(serviceInterface);
        if (serviceInfo == null) {
            webLog.warn("Service not found for route: {} ", route);
            return;
        }
        val method = route.getMethod();
        if (StringUtils.isNotBlank(method)) {
            val cfgName = serviceInfo.getCfgName();
            val serviceName = serviceInfo.getServiceName();
            val definition = BeanDefinitionBuilder.genericBeanDefinition(GtwRouterFunctionFactoryBean.class);

            definition.addConstructorArgValue(route);
            definition.addConstructorArgValue(serviceInterface);
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            val holder = getBeanDefinitionHolder(cfgName, serviceName + ".route." + route.index(), definition);
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        }
        // 铺平子路由
        if (route instanceof GroupRoute gRoute) {
            val routes = gRoute.getRoutes();
            if (!CollectionUtils.isEmpty(routes)) {
                for (Route subRoute : routes) {
                    subRoute.contextPath(route.contextPath());
                    registerRouterFunction(registry, serviceInterface, subRoute);
                }
            }
        }
    }

    private BeanDefinitionHolder getBeanDefinitionHolder(String appName, String serviceName,
            BeanDefinitionBuilder definition) {
        String[] qualifiers = new String[] { appName + "_" + serviceName };
        val beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, RouterFunction.class);
        boolean primary = true;
        beanDefinition.setPrimary(primary);

        return new BeanDefinitionHolder(beanDefinition, appName + "." + serviceName + ".RouterFunctionFactoryBean",
                qualifiers);
    }

}
