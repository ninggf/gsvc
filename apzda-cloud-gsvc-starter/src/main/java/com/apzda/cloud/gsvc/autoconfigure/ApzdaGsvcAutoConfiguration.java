package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.config.ServiceConfig;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.core.DefaultServiceCaller;
import com.apzda.cloud.gsvc.core.GatewayServiceBeanFactoryPostProcessor;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.TransHeadersPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

import java.util.Map;

/**
 * @author fengz
 */
@AutoConfiguration(before = { WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
@Import({ ApzdaGsvcWebConfig.class })
@Slf4j
public class ApzdaGsvcAutoConfiguration {

    @Bean
    GatewayServiceConfigure gatewayServiceConfigure(ServiceConfigProperties properties) {
        return new GatewayServiceConfigure(properties);
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
            val services = serviceConfigProperties.getService();
            for (Map.Entry<String, ServiceConfig> svc : services.entrySet()) {
                val svcName = svc.getKey();
                val service = svc.getValue();
                val interfaceName = service.getInterfaceName();
                val bean = applicationContext.getBean(interfaceName);
                GatewayServiceRegistry.setBean(interfaceName, bean);

                // 插件
                val methods = GatewayServiceRegistry.getDeclaredServiceMethods(interfaceName);
                for (Map.Entry<String, ServiceMethod> mv : methods.entrySet()) {
                    val method = mv.getValue();
                    val plugins = gatewayServiceConfigure.getPlugins(svcName, method.getDmName());
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
