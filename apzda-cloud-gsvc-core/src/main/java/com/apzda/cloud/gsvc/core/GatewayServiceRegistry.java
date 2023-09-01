package com.apzda.cloud.gsvc.core;

import io.grpc.MethodDescriptor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.style.ToStringCreator;

import java.lang.reflect.InvocationTargetException;
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

    private static final Map<String, Map<String, ServiceMethod>> DECLARED = new HashMap<>();

    private static final Map<Integer, ServiceInfo> SERVICE_INFO = new HashMap<>();

    @SuppressWarnings(("unchecked"))
    public static void register(String appName, String serviceName, int serviceIndex, Object bean,
            Class<?> interfaceName) {

        String serviceId = serviceName + "@" + appName;
        if (SERVICES.containsKey(serviceId)) {
            return;
        }
        val serviceMetaCls = interfaceName.getCanonicalName() + "Gsvc";

        try {
            val metaMethod = Class.forName(serviceMetaCls).getMethod("getMetadata", String.class);
            val hm = new HashMap<String, ServiceMethod>();

            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val meta = (Object[]) metaMethod.invoke(null, dmName);
                val methodInfo = new ServiceMethod(dm, appName, serviceName, serviceIndex, meta, bean);
                if (bean != null && log.isDebugEnabled()) {
                    if (serviceIndex != -1 && !SERVICE_INFO.getOrDefault(serviceIndex, ServiceInfo.DEFAULT).local) {
                        log.debug("Will Proxy method call: {}@{}/{}", serviceName, appName, dmName);
                    }
                }
                hm.put(dmName, methodInfo);
            }
            SERVICES.put(serviceId, hm);
        }
        catch (ClassNotFoundException e) {
            log.warn("Gsvc class {} not found for service '{}'", serviceMetaCls, serviceId);
        }
        catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.warn("Gsvc class {} is invalid for service '{}': {}", serviceMetaCls, serviceId, e.getMessage());
        }
    }

    public static ServiceMethod getServiceMethod(String appName, String serviceName, String methodName) {
        String serviceId = serviceName + "@" + appName;
        return SERVICES.getOrDefault(serviceId, Collections.emptyMap()).get(methodName);
    }

    public static Map<String, ServiceMethod> getServiceMethods(String appName, String serviceName) {
        String serviceId = serviceName + "@" + appName;
        return SERVICES.getOrDefault(serviceId, Collections.emptyMap());
    }

    public static ServiceMethod fromDeclaredMethod(ServiceMethod m) {
        return getServiceMethod(m.appName, m.serviceName, m.dmName);
    }

    public static void registerServiceInfo(ServiceInfo serviceInfo) {
        SERVICE_INFO.put(serviceInfo.index, serviceInfo);
        val methods = genDeclaredServiceMethods(serviceInfo.appName, serviceInfo.serviceName, serviceInfo.clazz);
        DECLARED.put(serviceInfo.appName + "@" + serviceInfo.serviceName, methods);
    }

    public static ServiceInfo getServiceInfo(Integer serviceIndex) {
        return SERVICE_INFO.get(serviceIndex);
    }

    public static boolean isLocalService(int index) {
        return SERVICE_INFO.getOrDefault(index, ServiceInfo.DEFAULT).local;
    }

    public static Map<String, ServiceMethod> getDeclaredServiceMethods(ServiceInfo serviceInfo) {
        return getDeclaredServiceMethods(serviceInfo.appName, serviceInfo.serviceName);
    }

    public static Map<String, ServiceMethod> getDeclaredServiceMethods(String app, String service) {
        return DECLARED.getOrDefault(app + "@" + service, Collections.emptyMap());
    }

    private static Map<String, ServiceMethod> genDeclaredServiceMethods(String app, String service,
            Class<?> interfaceName) {
        val serviceMetaCls = interfaceName.getCanonicalName() + "Gsvc";
        val hm = new HashMap<String, ServiceMethod>();
        try {
            val metaMethod = Class.forName(serviceMetaCls).getMethod("getMetadata", String.class);

            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val meta = (Object[]) metaMethod.invoke(null, dmName);
                val methodInfo = new ServiceMethod(dm, app, service, -1, meta, null);
                hm.put(dmName, methodInfo);
            }
        }
        catch (ClassNotFoundException e) {
            log.warn("Gsvc class {} not found ", serviceMetaCls);
        }
        catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.warn("Gsvc class {} is invalid: {}", serviceMetaCls, e.getMessage());
        }
        return hm;
    }

    @Getter
    public static class ServiceMethod {

        private final Method method;

        private final String appName;

        private final String serviceName;

        private final String dmName;

        private final Object bean;

        private final Object[] meta;

        private final Class<?> returnType;

        private final Class<?> requestType;

        private final MethodDescriptor.MethodType type;

        private final int serviceIndex;

        private Class<?> currentUserClz;

        public ServiceMethod(Method method, String appName, String serviceName, int serviceIndex, Object[] meta,
                Object bean) {
            this.method = method;
            this.appName = appName;
            this.serviceName = serviceName;
            this.serviceIndex = serviceIndex;
            this.meta = meta;
            this.bean = bean;
            this.dmName = method.getName();
            this.returnType = (Class<?>) meta[2];
            this.requestType = (Class<?>) meta[1];
            this.type = (MethodDescriptor.MethodType) meta[0];
            try {
                currentUserClz = this.requestType.getMethod("getCurrentUser").getReturnType();
                if (log.isTraceEnabled()) {
                    log.trace("{} need CurrentUser instance", this);
                }
            }
            catch (NoSuchMethodException e) {
                currentUserClz = null;
            }
        }

        public Object call(Object request) throws InvocationTargetException, IllegalAccessException {
            return method.invoke(bean, request);
        }

        public Class<?> reqClass() {
            return requestType;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this).append("appName", appName)
                .append("serviceName", serviceName)
                .append("method", dmName)
                .toString();
        }

    }

    @Getter
    @Builder
    public static class ServiceInfo {

        private int index;

        private String appName;

        private String serviceName;

        private String contextPath;

        private Class<?> clazz;

        private boolean local;

        public static final ServiceInfo DEFAULT = ServiceInfo.builder().build();

    }

}
