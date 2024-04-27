package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.resolver.NoneResolver;
import com.apzda.cloud.gsvc.resolver.ServiceNameResolver;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.List;

/**
 * @author ninggf
 */
@RequiredArgsConstructor
public class GatewayServiceConfigure implements IServiceConfigure {

    private final ServiceConfigProperties serviceConfig;

    private final ObjectProvider<IGtwGlobalFilter<ServerResponse, ServerResponse>> globalFilters;

    private final ObjectProvider<IGlobalPlugin> globalPlugins;

    /**
     * 仅供GRPC stub使用.
     * @param cfgName 服务配置名
     * @return 服务发现名称
     * @deprecated
     */
    @Deprecated
    public String getSvcName(String cfgName) {
        return StringUtils.defaultIfBlank(serviceConfig.refConfig(cfgName).getSvcName(), cfgName);
    }

    public Duration getReadTimeout(String svcName, boolean isRef) {
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        // service
        var readTimeout = config.getReadTimeout();
        if (readTimeout.toMillis() > 0) {
            return readTimeout;
        }
        // default
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        readTimeout = config.getReadTimeout();
        if (readTimeout.toMillis() > 0) {
            return readTimeout;
        }

        return isRef ? Duration.ofSeconds(60) : Duration.ZERO;
    }

    @Override
    public Duration getReadTimeout(ServiceMethod method, boolean isRef) {
        val svcName = method.getCfgName();
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        val dmName = method.getDmName();
        val methodConfig = config.getMethods().get(dmName);
        if (methodConfig != null) {
            val readTimeout = methodConfig.getReadTimeout();
            if (readTimeout.toMillis() > 0) {
                return readTimeout;
            }
        }
        return getReadTimeout(svcName, isRef);
    }

    public Duration getReadTimeout(ServiceMethod method) {
        val svcName = method.getCfgName();
        var config = serviceConfig.refConfig(svcName);
        val dmName = method.getDmName();
        val methodConfig = config.getMethods().get(dmName);
        if (methodConfig != null) {
            val readTimeout = methodConfig.getReadTimeout();
            if (readTimeout.toMillis() > 0) {
                return readTimeout;
            }
        }
        return Duration.ZERO;
    }

    public Duration getWriteTimeout(String svcName, boolean isRef) {
        // service > default
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        var writeTimeout = config.getWriteTimeout();
        if (writeTimeout.toMillis() > 0) {
            return writeTimeout;
        }
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        writeTimeout = config.getWriteTimeout();
        if (writeTimeout.toMillis() > 0) {
            return writeTimeout;
        }

        return Duration.ZERO;
    }

    public Duration getConnectTimeout(String svcName) {
        var config = serviceConfig.refConfig(svcName);
        // service > default
        var connectTimeout = config.getConnectTimeout();
        if (connectTimeout.toMillis() > 0) {
            return connectTimeout;
        }
        config = serviceConfig.refConfig("default");
        connectTimeout = config.getConnectTimeout();
        if (connectTimeout.toMillis() > 0) {
            return connectTimeout;
        }
        // default
        return Duration.ofSeconds(3);
    }

    public Duration getTimeout(String svcName, String method) {
        // method > service > default
        var config = serviceConfig.svcConfig(svcName);
        val methodConfig = config.getMethods().get(method);
        if (methodConfig != null && methodConfig.getTimeout().toMillis() > 0) {
            return methodConfig.getTimeout();
        }

        var timeout = config.getTimeout();
        if (timeout.toMillis() > 0) {
            return timeout;
        }
        config = serviceConfig.svcConfig("default");
        timeout = config.getTimeout();
        if (timeout.toMillis() > 0) {
            return timeout;
        }
        // default
        return Duration.ZERO;
    }

    public Duration getGrpcKeepAliveTime(String svcName, boolean isRef) {
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        // service global
        var keepAliveTime = config.getGrpc().getKeepAliveTime();
        if (keepAliveTime.toMillis() > 0) {
            return keepAliveTime;
        }
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        keepAliveTime = config.getGrpc().getKeepAliveTime();
        if (keepAliveTime.toMillis() > 0) {
            return keepAliveTime;
        }
        // default
        return Duration.ofSeconds(300);
    }

    public Duration getGrpcKeepAliveTimeout(String svcName, boolean isRef) {
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        // service global
        var keepAliveTimeout = config.getGrpc().getKeepAliveTimeout();
        if (keepAliveTimeout.toMillis() > 0) {
            return keepAliveTimeout;
        }
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        keepAliveTimeout = config.getGrpc().getKeepAliveTimeout();
        if (keepAliveTimeout.toMillis() > 0) {
            return keepAliveTimeout;
        }
        // default
        return Duration.ofSeconds(5);
    }

    public String svcLbName(String cfgName) {
        String serviceName = cfgName;
        if (GatewayServiceRegistry.SERVICE_ALIAS.containsKey(cfgName)) {
            serviceName = GatewayServiceRegistry.SERVICE_ALIAS.get(cfgName);
        }
        var svcLbName = serviceConfig.refConfig(serviceName).getSvcName();
        svcLbName = StringUtils.defaultIfBlank(svcLbName, serviceName);

        val registry = serviceConfig.getRegistry();
        val type = registry.getType();
        val resolver = ServiceNameResolver.RESOLVERS.getOrDefault(type, new NoneResolver());

        return resolver.resolve(svcLbName, registry);
    }

    public List<String> getPlugins(String svcName, String methodName, boolean isRef) {
        // global
        var config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        val plugins = config.getPlugins();
        // service
        config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        for (String plugin : config.getPlugins()) {
            if (StringUtils.startsWith(plugin, "-")) {
                plugins.remove(plugin.substring(1));
            }
            else {
                plugins.add(plugin);
            }
        }
        // method
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            for (String plugin : methodConfig.getPlugins()) {
                if (StringUtils.startsWith(plugin, "-")) {
                    plugins.remove(plugin.substring(1));
                }
                else {
                    plugins.add(plugin);
                }
            }
        }
        // default
        return plugins;
    }

    public List<IGtwGlobalFilter<ServerResponse, ServerResponse>> getGlobalFilters() {
        return globalFilters.orderedStream().toList();
    }

    public List<? extends IPlugin> getGlobalPlugins() {
        return globalPlugins.orderedStream().toList();
    }

    public boolean isFlatResponse() {
        return serviceConfig.getConfig().isFlatResponse();
    }

}
