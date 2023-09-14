package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.gtw.RouteMeta;
import com.google.common.base.Splitter;
import io.grpc.ServiceDescriptor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz
 */
@Slf4j
public class GatewayServiceRegistry {

    private static final Map<String, Map<String, ServiceMethod>> SERVICES = new HashMap<>();

    private static final Map<String, Map<String, ServiceMethod>> SERVICE_METHODS = new HashMap<>();

    public static final Map<Class<?>, ServiceInfo> DECLARED_SERVICES = new HashMap<>();

    public static final Map<Class<?>, ServiceDescriptor> SERVCIE_DESCRIPTOR = new HashMap<>();

    public static final Map<String, RouteMeta> AUTHED_ROUTES = new HashMap<>();

    public static void register(Class<?> interfaceName) {
        if (!interfaceName.isInterface()) {
            throw new IllegalArgumentException(String.format("%s is not interface", interfaceName));
        }
        DECLARED_SERVICES.computeIfAbsent(interfaceName, key -> {
            val svcName = svcName(interfaceName);
            val cfgName = cfgName(interfaceName);
            return ServiceInfo.builder().clazz(interfaceName).serviceName(svcName).cfgName(cfgName).local(true).build();
        });
    }

    public static void registerProxy(Class<?> interfaceName) {
        if (!interfaceName.isInterface()) {
            throw new IllegalArgumentException(String.format("%s is not interface", interfaceName));
        }
        register(interfaceName);
        DECLARED_SERVICES.get(interfaceName).local = false;
    }

    public static void register(Class<?> interfaceName, Map<String, Object[]> methodMeta) {
        register(interfaceName);
        genDeclaredServiceMethods(interfaceName, methodMeta);
    }

    public static void register(Class<?> interfaceName, ServiceDescriptor descriptor) {
        SERVCIE_DESCRIPTOR.putIfAbsent(interfaceName, descriptor);
    }

    public static void setBean(Class<?> interfaceName, Object bean, boolean local) {
        val cfgName = cfgName(interfaceName);
        val serviceName = svcName(interfaceName);
        SERVICES.computeIfAbsent(serviceName + "@" + cfgName, (key) -> {
            val hm = new HashMap<String, ServiceMethod>();
            log.debug("Inject {} Bean into Service: {} - {}", local ? "Impl" : "Stub", serviceName, bean);
            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val serviceMethod = SERVICE_METHODS.getOrDefault(cfgName + "@" + serviceName, Collections.emptyMap())
                    .get(dmName);

                if (serviceMethod == null) {
                    log.warn("Service method not found: {}.{}", serviceName, dmName);
                    continue;
                }

                serviceMethod.setBean(bean);
                hm.put(dmName, serviceMethod);
            }
            return hm;
        });
    }

    public static Map<String, ServiceMethod> getServiceMethods(String cfgName, String serviceName) {
        String serviceId = serviceName + "@" + cfgName;
        return SERVICES.getOrDefault(serviceId, Collections.emptyMap());
    }

    public static ServiceInfo getServiceInfo(Class<?> clazz) {
        return DECLARED_SERVICES.get(clazz);
    }

    public static Map<String, ServiceMethod> getDeclaredServiceMethods(ServiceInfo serviceInfo) {
        return getDeclaredServiceMethods(serviceInfo.cfgName, serviceInfo.serviceName);
    }

    public static Map<String, ServiceMethod> getDeclaredServiceMethods(Class<?> clazz) {
        val serviceInfo = getServiceInfo(clazz);
        return getDeclaredServiceMethods(serviceInfo.cfgName, serviceInfo.serviceName);
    }

    public static ServiceMethod getServiceMethod(Class<?> clazz, String method) {
        val serviceInfo = getServiceInfo(clazz);
        val cfg = serviceInfo.cfgName;
        val service = serviceInfo.serviceName;
        return SERVICE_METHODS.getOrDefault(cfg + "@" + service, Collections.emptyMap()).get(method);
    }

    public static Map<String, ServiceMethod> getDeclaredServiceMethods(String cfgName, String serviceName) {
        return SERVICE_METHODS.getOrDefault(cfgName + "@" + serviceName, Collections.emptyMap());
    }

    public static String cfgName(Class<?> clazz) {
        return cfgName(clazz.getSimpleName());
    }

    public static String cfgName(String serviceName) {
        val names = Splitter.on(".").trimResults().omitEmptyStrings().splitToList(serviceName);
        var cfgName = names.get(names.size() - 1);
        cfgName = Character.toUpperCase(cfgName.charAt(0)) + cfgName.substring(1);
        return cfgName.replaceFirst("Gsvc", "");
    }

    public static void registerRouteMeta(String url, RouteMeta meta) {
        Assert.notNull(meta, "meta cannot be null for path: " + url);
        AUTHED_ROUTES.put(url, meta);
    }

    public static String svcName(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        val simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    static void genDeclaredServiceMethods(Class<?> interfaceName, Map<String, Object[]> methodsMeta) {
        val cfgName = cfgName(interfaceName);
        val service = svcName(interfaceName);
        SERVICE_METHODS.computeIfAbsent(cfgName + "@" + service, key -> {
            val hm = new HashMap<String, ServiceMethod>();
            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val meta = methodsMeta.get(dmName);
                if (meta == null) {
                    continue;
                }
                val methodInfo = new ServiceMethod(dm, cfgName, service, meta, null);
                hm.put(dmName, methodInfo);
            }
            return hm;
        });
    }

}
