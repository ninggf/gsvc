package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.core.*;
import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.TransHeadersPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;

/**
 * @author fengz
 */
@AutoConfiguration(before = { WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
@Import({ ApzdaGsvcWebConfig.class, SentinelAutoConfiguration.class })
@Slf4j
public class ApzdaGsvcAutoConfiguration {

    @Bean
    GatewayServiceConfigure gatewayServiceConfigure(ServiceConfigProperties properties,
            ObjectProvider<List<IGtwGlobalFilter<ServerResponse, ServerResponse>>> globalFilters,
            ObjectProvider<List<IGlobalPlugin>> globalPlugins) {
        return new GatewayServiceConfigure(properties, globalFilters, globalPlugins);
    }

    @Bean
    @ConditionalOnMissingBean
    WebClient webClient(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        return WebClient.builder().filter(lbFunction).build();
    }

    @Bean
    @ConditionalOnMissingBean
    IServiceCaller serviceCaller(ApplicationContext applicationContext, WebClient webClient,
            GatewayServiceConfigure serviceConfigure) {
        return new DefaultServiceCaller(applicationContext, webClient, serviceConfigure);
    }

    @Bean
    TransHeadersPlugin transHeadersPlugin(ApplicationContext applicationContext) {
        return new TransHeadersPlugin(applicationContext);
    }

    @Bean
    BeanFactoryPostProcessor gsvcBeanFactoryPostProcessor() {
        return new GatewayServiceBeanFactoryPostProcessor();
    }

    @Configuration
    @RequiredArgsConstructor
    public static class GsvcServer implements SmartLifecycle {

        private final ApplicationContext applicationContext;

        private final ServiceConfigProperties serviceConfigProperties;

        private final GatewayServiceConfigure gatewayServiceConfigure;

        private volatile boolean running = false;

        @Override
        public void start() {
            val services = GatewayServiceRegistry.DECLARED_SERVICES;
            val globalPlugins = gatewayServiceConfigure.getGlobalPlugins();
            for (Map.Entry<Class<?>, ServiceInfo> svc : services.entrySet()) {
                val service = svc.getValue();
                val cfgName = service.getCfgName();
                val interfaceName = service.getClazz();
                val svcLbName = gatewayServiceConfigure.svcLbName(cfgName);
                val bean = applicationContext.getBean(interfaceName);
                GatewayServiceRegistry.setBean(interfaceName, bean, service.isLocal());

                // setup plugin
                val methods = GatewayServiceRegistry.getDeclaredServiceMethods(interfaceName);
                for (Map.Entry<String, ServiceMethod> mv : methods.entrySet()) {
                    val method = mv.getValue();
                    method.setSvcLbName(svcLbName);
                    for (IGlobalPlugin plugin : globalPlugins) {
                        method.registerPlugin(plugin);
                    }
                    val plugins = gatewayServiceConfigure.getPlugins(cfgName, method.getDmName(), !service.isLocal());
                    for (String plugin : plugins) {
                        method.registerPlugin(applicationContext.getBean(plugin, IPlugin.class));
                    }
                }
            }
            running = true;
        }

        @Override
        public void stop() {
            // nothing to do
        }

        @Override
        public boolean isRunning() {
            return running;
        }

    }

}
