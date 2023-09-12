package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author ninggf
 */
@RequiredArgsConstructor
public class GatewayServiceConfigure {

    private final ServiceConfigProperties serviceConfig;

    private final ObjectProvider<List<IGtwGlobalFilter<ServerResponse, ServerResponse>>> globalFilters;

    private final ObjectProvider<List<IGlobalPlugin>> globalPlugins;

    public Duration getReadTimeout(String svcName, String methodName, boolean isRef) {
        val config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        // 方法级
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            val mrTimeout = methodConfig.getReadTimeout();
            if (!mrTimeout.isZero()) {
                return mrTimeout;
            }
        }
        // service global
        val readTimeout = config.getReadTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        return Duration.ofSeconds(3600);
    }

    public Duration getConnectTimeout(String svcName, String methodName) {
        val config = serviceConfig.refConfig(svcName);
        // method
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            val mrTimeout = methodConfig.getConnectTimeout();
            if (!mrTimeout.isZero()) {
                return mrTimeout;
            }
        }
        // service global
        val readTimeout = config.getConnectTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        return Duration.ofSeconds(5);
    }

    public Duration getTimeout(String svcName, String methodName) {
        val config = serviceConfig.svcConfig(svcName);
        // method
        val methodConfig = config.getMethods().get(methodName);
        if (methodConfig != null) {
            val mrTimeout = methodConfig.getTimeout();
            if (!mrTimeout.isZero()) {
                return mrTimeout;
            }
        }
        // service global
        val readTimeout = config.getTimeout();
        if (!readTimeout.isZero()) {
            return readTimeout;
        }
        // default
        return Duration.ofSeconds(3610);
    }

    public String svcLbName(String cfgName) {
        var svcLbName = serviceConfig.refConfig(cfgName).getSvcName();
        svcLbName = StringUtils.defaultIfBlank(svcLbName, cfgName);

        return Character.toLowerCase(svcLbName.charAt(0)) + svcLbName.substring(1);
    }

    public List<String> getPlugins(String svcName, String methodName, boolean isRef) {
        val config = isRef ? serviceConfig.refConfig(svcName) : serviceConfig.svcConfig(svcName);
        val plugins = config.getPlugins();
        // 方法级
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

    public List<IGlobalPlugin> getGlobalPlugins() {
        val plugins = globalPlugins.getIfAvailable();
        if (plugins != null) {
            plugins.sort(Comparator.comparingInt(IGlobalPlugin::getOrder));
            return plugins;
        }
        return Collections.emptyList();
    }

}