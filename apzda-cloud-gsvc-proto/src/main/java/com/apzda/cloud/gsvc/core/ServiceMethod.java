/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.plugin.IPlugin;
import io.grpc.MethodDescriptor;
import lombok.Getter;
import org.springframework.core.style.ToStringCreator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author fengz
 */
@Getter
public class ServiceMethod {

    private static final Pattern SVC_NAME_PATTERN = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);

    private final Method method;

    private final String cfgName;

    private final String serviceName;

    private final String dmName;

    private final String clientBeanName;

    private final Class<?> interfaceName;

    private final Object[] meta;

    private final String rpcAddr;

    private final Class<?> returnType;

    private final Class<?> requestType;

    private final MethodDescriptor.MethodType type;

    private final List<IPlugin> plugins = new ArrayList<>();

    private Object bean;

    public ServiceMethod(Method method, String cfgName, String serviceName, Object[] meta, Object bean) {
        this.interfaceName = method.getDeclaringClass();
        this.method = method;
        this.cfgName = cfgName;
        this.clientBeanName = cfgName + "WebClient";
        this.serviceName = serviceName;
        this.meta = meta;
        this.bean = bean;
        this.dmName = method.getName();
        this.rpcAddr = String.format("/~%s/%s", serviceName, dmName);
        this.returnType = (Class<?>) meta[2];
        this.requestType = (Class<?>) meta[1];
        this.type = (MethodDescriptor.MethodType) meta[0];
    }

    void setBean(Object bean) {
        this.bean = bean;
    }

    public void registerPlugin(IPlugin plugin) {
        plugins.add(plugin);
    }

    public Object call(Object request) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(bean, request);
    }

    public Class<?> reqClass() {
        return requestType;
    }

    public static String baseUrl(String svcLbName) {
        if (SVC_NAME_PATTERN.matcher(svcLbName).matches()) {
            return svcLbName;
        }
        else {
            return String.format("http://%s", svcLbName);
        }
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("serviceName", serviceName)
            .append("method", dmName)
            .append("bean", bean)
            .toString();
    }

}
