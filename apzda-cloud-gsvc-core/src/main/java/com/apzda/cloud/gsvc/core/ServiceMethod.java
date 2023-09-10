/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.TransHeadersPlugin;
import io.grpc.MethodDescriptor;
import lombok.Getter;
import org.springframework.core.style.ToStringCreator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fengz
 */
@Getter
public class ServiceMethod {

    private final Method method;

    private final String appName;

    private final String serviceName;

    private final String dmName;

    private final String rpcAddr;

    private final Class<?> interfaceName;

    private final Object[] meta;

    private final Class<?> returnType;

    private final Class<?> requestType;

    private final MethodDescriptor.MethodType type;

    private final List<IPlugin> plugins = new ArrayList<>();

    private Object bean;

    private Class<?> currentUserClz;

    public ServiceMethod(Method method, String appName, String serviceName, Object[] meta, Object bean) {
        this.interfaceName = method.getDeclaringClass();
        this.method = method;
        this.appName = appName;
        this.serviceName = serviceName;
        this.meta = meta;
        this.bean = bean;
        this.dmName = method.getName();
        this.rpcAddr = String.format("http://%s/%s/%s", serviceName, serviceName, dmName);
        this.returnType = (Class<?>) meta[2];
        this.requestType = (Class<?>) meta[1];
        this.type = (MethodDescriptor.MethodType) meta[0];
        try {
            currentUserClz = this.requestType.getMethod("getCurrentUser").getReturnType();
        }
        catch (NoSuchMethodException e) {
            currentUserClz = null;
        }
        plugins.add(TransHeadersPlugin.TRANS_HEADERS_PLUGIN);
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

    @Override
    public String toString() {
        return new ToStringCreator(this).append("appName", appName)
            .append("serviceName", serviceName)
            .append("method", dmName)
            .append("bean", bean)
            .toString();
    }

}
