package com.apzda.cloud.gsvc.core;

import io.grpc.MethodDescriptor;
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
    private static final Map<String, Map<String, MethodInfo>> SERVICES = new HashMap<>();
    private static final Map<String, Boolean> LOCAL_IMPLS = new HashMap<>();
    private static final Map<Integer, Boolean> LOCAL_IMPLS_BY_INDEX = new HashMap<>();

    @SuppressWarnings(("unchecked"))
    public static void register(String appName, String serviceName, int serviceIndex, Object bean, Class<?> interfaceName) {

        String serviceId = serviceName + "@" + appName;
        if (SERVICES.containsKey(serviceId)) {
            return;
        }
        val serviceMetaCls = interfaceName.getCanonicalName() + "Gsvc";

        try {
            val metaMethod = Class.forName(serviceMetaCls).getMethod("getMetadata", String.class);
            val hm = new HashMap<String, MethodInfo>();

            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val meta = (Object[]) metaMethod.invoke(null, dmName);
                val methodInfo = new MethodInfo(dm, appName, serviceName, serviceIndex, meta, bean);
                if (bean != null && log.isDebugEnabled()) {
                    if (!LOCAL_IMPLS_BY_INDEX.getOrDefault(serviceIndex, false)) {
                        log.debug("Will Proxy method call: {}@{}/{}", serviceName, appName, dmName);
                    }
                }
                hm.put(dmName, methodInfo);
            }
            SERVICES.put(serviceId, hm);
        } catch (ClassNotFoundException e) {
            log.warn("Gsvc class {} not found for service '{}'", serviceMetaCls, serviceId);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.warn("Gsvc class {} is invalid for service '{}': {}", serviceMetaCls, serviceId, e.getMessage());
        }
    }

    public static MethodInfo getServiceMethod(String appName, String serviceName, String methodName) {
        String serviceId = serviceName + "@" + appName;
        return SERVICES.getOrDefault(serviceId, Collections.emptyMap()).get(methodName);
    }

    public static Map<String, MethodInfo> getServiceMethods(String appName, String serviceName) {
        String serviceId = serviceName + "@" + appName;
        return SERVICES.getOrDefault(serviceId, Collections.emptyMap());
    }

    public static MethodInfo getServiceMethod(MethodInfo m) {
        return getServiceMethod(m.appName, m.serviceName, m.dmName);
    }

    public static void markLocalService(String appName, String serviceName, Integer index) {
        String serviceId = serviceName + "@" + appName;
        LOCAL_IMPLS.put(serviceId, true);
        LOCAL_IMPLS_BY_INDEX.put(index, true);
    }

    public static boolean isLocalService(String appName, String serviceName) {
        String serviceId = serviceName + "@" + appName;
        return LOCAL_IMPLS.getOrDefault(serviceId, false);
    }

    public static boolean isLocalService(int index) {
        return LOCAL_IMPLS_BY_INDEX.getOrDefault(index, false);
    }

    public static Map<String, MethodInfo> getServiceMethods(String app, String service, Class<?> interfaceName) {
        val serviceMetaCls = interfaceName.getCanonicalName() + "Gsvc";
        val hm = new HashMap<String, MethodInfo>();
        try {
            val metaMethod = Class.forName(serviceMetaCls).getMethod("getMetadata", String.class);

            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val meta = (Object[]) metaMethod.invoke(null, dmName);
                val methodInfo = new MethodInfo(dm, app, service, -1, meta, null);
                hm.put(dmName, methodInfo);
            }
        } catch (ClassNotFoundException e) {
            log.warn("Gsvc class {} not found ", serviceMetaCls);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.warn("Gsvc class {} is invalid: {}", serviceMetaCls, e.getMessage());
        }
        return hm;
    }

    @Getter
    public static class MethodInfo {
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

        public MethodInfo(Method method, String appName, String serviceName, int serviceIndex, Object[] meta, Object bean) {
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
            } catch (NoSuchMethodException e) {
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
            return new ToStringCreator(this)
                .append("appName", appName)
                .append("serviceName", serviceName)
                .append("method", dmName)
                .toString();
        }
    }
}
