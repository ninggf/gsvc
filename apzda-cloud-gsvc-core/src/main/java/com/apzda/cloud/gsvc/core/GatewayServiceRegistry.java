package com.apzda.cloud.gsvc.core;

import com.google.common.base.Splitter;
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

    private static final Map<String, Map<String, ServiceMethod>> SERVICE_METHODS = new HashMap<>();

    public static final Map<Class<?>, ServiceInfo> DECLARED_SERVICES = new HashMap<>();

    public static void register(Class<?> interfaceName) {
        DECLARED_SERVICES.computeIfAbsent(interfaceName, key -> {
            val svcName = svcName(interfaceName);
            val appName = shortSvcName(interfaceName);
            return ServiceInfo.builder()
                .clazz(interfaceName)
                .serviceName(svcName)
                .shortName(appName)
                .appName(appName)
                .build();
        });
    }

    public static void register(Class<?> interfaceName, Map<String, Object[]> methodMeta) {
        val app = shortSvcName(interfaceName);
        val svcName = svcName(interfaceName);
        genDeclaredServiceMethods(app, svcName, interfaceName, methodMeta);
    }

    public static void setBean(Class<?> interfaceName, Object bean) {
        val appName = shortSvcName(interfaceName);
        val serviceName = svcName(interfaceName);
        SERVICES.computeIfAbsent(serviceName + "@" + appName, (key) -> {
            val hm = new HashMap<String, ServiceMethod>();
            log.debug("Inject Bean into Service: {} - {}", serviceName, interfaceName);
            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val serviceMethod = SERVICE_METHODS.getOrDefault(appName + "@" + serviceName, Collections.emptyMap())
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

    public static Map<String, ServiceMethod> getServiceMethods(String appName, String serviceName) {
        String serviceId = serviceName + "@" + appName;
        return SERVICES.getOrDefault(serviceId, Collections.emptyMap());
    }

    public static ServiceInfo getServiceInfo(Class<?> clazz) {
        return DECLARED_SERVICES.get(clazz);
    }

    public static Map<String, ServiceMethod> getDeclaredServiceMethods(ServiceInfo serviceInfo) {
        return getDeclaredServiceMethods(serviceInfo.appName, serviceInfo.serviceName);
    }

    public static Map<String, ServiceMethod> getDeclaredServiceMethods(String app, String service) {
        return SERVICE_METHODS.getOrDefault(app + "@" + service, Collections.emptyMap());
    }

    public static String shortSvcName(Class<?> clazz) {
        val svcName = svcName(clazz);

        return svcName.replaceFirst("Gsvc", "").replace("Service", "").replaceAll("([A-Z])", "-$1").toLowerCase();
    }

    public static String shortSvcName(String name) {

        val names = Splitter.on(".").trimResults().omitEmptyStrings().splitToList(name);
        var svcName = names.get(names.size() - 1);
        svcName = Character.toLowerCase(svcName.charAt(0)) + svcName.substring(1);
        return svcName.replaceFirst("Gsvc", "").replaceFirst("Service", "").replaceAll("([A-Z])", "-$1").toLowerCase();
    }

    public static String svcName(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        val simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    static void genDeclaredServiceMethods(String app, String service, Class<?> interfaceName,
            Map<String, Object[]> methodsMeta) {
        SERVICE_METHODS.computeIfAbsent(app + "@" + service, key -> {
            val hm = new HashMap<String, ServiceMethod>();
            for (Method dm : interfaceName.getDeclaredMethods()) {
                val dmName = dm.getName();
                val meta = methodsMeta.get(dmName);
                if (meta == null) {
                    continue;
                }
                val methodInfo = new ServiceMethod(dm, app, service, meta, null);
                hm.put(dmName, methodInfo);
            }
            return hm;
        });
    }

    @Getter
    public static class ServiceMethod {

        private final Method method;

        private final String appName;

        private final String serviceName;

        private final String dmName;

        private Object bean;

        private final Object[] meta;

        private final Class<?> returnType;

        private final Class<?> requestType;

        private final MethodDescriptor.MethodType type;

        private Class<?> currentUserClz;

        public ServiceMethod(Method method, String appName, String serviceName, Object[] meta, Object bean) {
            this.method = method;
            this.appName = appName;
            this.serviceName = serviceName;
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

        void setBean(Object bean) {
            this.bean = bean;
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
                .append("bean", bean)
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

        private String shortName;

        private Class<?> clazz;

        private boolean local;

        public static final ServiceInfo DEFAULT = ServiceInfo.builder().build();

    }

}
