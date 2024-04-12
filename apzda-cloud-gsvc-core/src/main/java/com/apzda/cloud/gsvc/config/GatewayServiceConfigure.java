package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.resolver.NoneResolver;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author ninggf
 */
@RequiredArgsConstructor
public class GatewayServiceConfigure implements IServiceConfigure {

    private final ServiceConfigProperties serviceConfig;

    private final ObjectProvider<List<IGtwGlobalFilter<ServerResponse, ServerResponse>>> globalFilters;

    private final ObjectProvider<List<IGlobalPlugin>> globalPlugins;

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
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        readTimeout = config.getReadTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }

        return Duration.ofSeconds(30);
    }

    public Duration getWriteTimeout(String svcName, boolean isRef) {
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        var writeTimeout = config.getWriteTimeout();
        if (!writeTimeout.isZero()) {
            return writeTimeout;
        }
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        writeTimeout = config.getWriteTimeout();
        if (!writeTimeout.isZero()) {
            return writeTimeout;
        }
        // service global
        return Duration.ZERO;
    }

    public Duration getConnectTimeout(String svcName) {
        var config = serviceConfig.refConfig(svcName);
        // service global
        var readTimeout = config.getConnectTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        config = serviceConfig.refConfig("default");
        readTimeout = config.getConnectTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        return Duration.ofSeconds(1);
    }

    public Duration getTimeout(String svcName, String method) {
        var config = serviceConfig.svcConfig(svcName);
        val methodConfig = config.getMethods().get(method);
        if (methodConfig != null && !methodConfig.getTimeout().isZero()) {
            return methodConfig.getTimeout();
        }

        var timeout = config.getTimeout();
        if (!timeout.isZero()) {
            return timeout;
        }
        config = serviceConfig.svcConfig("default");
        timeout = config.getTimeout();
        if (!timeout.isZero()) {
            return timeout;
        }
        // default
        return Duration.ZERO;
    }

    public Duration getGrpcKeepAliveTime(String svcName, boolean isRef) {
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        // service global
        var keepAliveTime = config.getGrpc().getKeepAliveTime();
        if (!keepAliveTime.isZero()) {
            return keepAliveTime;
        }
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        keepAliveTime = config.getGrpc().getKeepAliveTime();
        if (!keepAliveTime.isZero()) {
            return keepAliveTime;
        }
        // default
        return Duration.ofSeconds(120);
    }

    public Duration getGrpcKeepAliveTimeout(String svcName, boolean isRef) {
        var config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        // service global
        var keepAliveTimeout = config.getGrpc().getKeepAliveTimeout();
        if (!keepAliveTimeout.isZero()) {
            return keepAliveTimeout;
        }
        config = isRef ? serviceConfig.refConfig("default") : serviceConfig.svcConfig("default");
        keepAliveTimeout = config.getGrpc().getKeepAliveTimeout();
        if (!keepAliveTimeout.isZero()) {
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
        val resolver = ServiceConfigProperties.RESOLVERS.getOrDefault(type, new NoneResolver());

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
        val filters = globalFilters.getIfAvailable();
        if (filters != null) {
            filters.sort(Comparator.comparingInt(IGtwGlobalFilter::getOrder));
            return filters;
        }
        return Collections.emptyList();
    }

    public List<? extends IPlugin> getGlobalPlugins() {
        val plugins = globalPlugins.getIfAvailable();
        // plugins.sort(Comparator.comparingInt(IGlobalPlugin::getOrder));
        return Objects.requireNonNullElse(plugins, Collections.emptyList());
    }

}
