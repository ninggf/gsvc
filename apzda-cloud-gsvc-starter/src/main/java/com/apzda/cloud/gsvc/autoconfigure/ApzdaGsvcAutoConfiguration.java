package com.apzda.cloud.gsvc.autoconfigure;

import build.buf.protovalidate.Config;
import build.buf.protovalidate.Validator;
import com.apzda.cloud.gsvc.client.DefaultRemoteServiceCaller;
import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.client.plugin.TransHeadersPlugin;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.core.GatewayServiceBeanFactoryPostProcessor;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.ServiceInfo;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.gtw.IGtwGlobalFilter;
import com.apzda.cloud.gsvc.gtw.ProxyExchangeHandler;
import com.apzda.cloud.gsvc.gtw.filter.HttpHeadersFilter;
import com.apzda.cloud.gsvc.gtw.filter.RemoveHopByHopHeadersFilter;
import com.apzda.cloud.gsvc.gtw.filter.TransferEncodingNormalizationHeadersFilter;
import com.apzda.cloud.gsvc.gtw.filter.XForwardedHeadersFilter;
import com.apzda.cloud.gsvc.infra.Counter;
import com.apzda.cloud.gsvc.infra.LocalInfraImpl;
import com.apzda.cloud.gsvc.plugin.IGlobalPlugin;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.security.config.GsvcSecurityAutoConfiguration;
import com.apzda.cloud.gsvc.server.DefaultServiceMethodHandler;
import com.apzda.cloud.gsvc.server.IServiceMethodHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;

/**
 * @author fengz
 */
@AutoConfiguration(before = { WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class,
        GsvcSecurityAutoConfiguration.class, WebClientAutoConfiguration.class })
@Import({ ApzdaGsvcWebConfig.class, SentinelAutoConfiguration.class, RedisInfraConfiguration.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApzdaGsvcAutoConfiguration {

    @Bean
    // @LoadBalanced
    @Scope("prototype")
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder(ObjectProvider<WebClientCustomizer> customizerProvider,
            ObjectProvider<DeferringLoadBalancerExchangeFilterFunction<? extends ExchangeFilterFunction>> filterFunctionObjectProvider) {
        val builder = WebClient.builder();
        customizerProvider.orderedStream().forEach((customizer) -> customizer.customize(builder));

        filterFunctionObjectProvider.orderedStream().forEach(filter -> {
            if (log.isTraceEnabled()) {
                log.trace("{} is used by WebClient.builder", filter.getClass().getCanonicalName());
            }
            builder.filter(filter);
        });

        return builder;
    }

    @Bean
    GatewayServiceConfigure gatewayServiceConfigure(ServiceConfigProperties properties,
            ObjectProvider<List<IGtwGlobalFilter<ServerResponse, ServerResponse>>> globalFilters,
            ObjectProvider<List<IGlobalPlugin>> globalPlugins) {
        return new GatewayServiceConfigure(properties, globalFilters, globalPlugins);
    }

    @Bean
    @ConditionalOnMissingBean
    IServiceCaller serviceCaller(ApplicationContext applicationContext, GatewayServiceConfigure serviceConfigure) {
        return new DefaultRemoteServiceCaller(applicationContext, serviceConfigure);
    }

    @Bean
    TransHeadersPlugin transHeadersPlugin(ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider) {
        return new TransHeadersPlugin(headersFiltersProvider);
    }

    @Bean
    HttpHeadersFilter removeHopByHopHeadersFilter() {
        return new RemoveHopByHopHeadersFilter();
    }

    @Bean
    HttpHeadersFilter transferEncodingNormalizationHeadersFilter() {
        return new TransferEncodingNormalizationHeadersFilter();
    }

    @Bean
    HttpHeadersFilter xForwardedHeadersFilter() {
        return new XForwardedHeadersFilter();
    }

    @Bean
    BeanFactoryPostProcessor gsvcBeanFactoryPostProcessor() {
        return new GatewayServiceBeanFactoryPostProcessor();
    }

    @Bean
    Validator protobufValidator() {
        val config = Config.newBuilder();
        config.setFailFast(false);
        return new Validator(config.build());
    }

    @Bean
    @ConditionalOnMissingClass("org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnMissingBean
    LocalInfraImpl infraCounterAndStorage(ServiceConfigProperties properties) {
        return new LocalInfraImpl(properties.getConfig().getTempExpireTime());
    }

    @Bean
    @ConditionalOnMissingBean
    IServiceMethodHandler serviceMethodHandler(GatewayServiceConfigure serviceConfigure, ObjectMapper objectMapper,
            GsvcExceptionHandler gsvcExceptionHandler, Validator validator, MultipartResolver multipartResolver) {
        return new DefaultServiceMethodHandler(serviceConfigure, objectMapper, gsvcExceptionHandler, validator,
                multipartResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    ProxyExchangeHandler proxyExchangeHandler(GatewayServiceConfigure serviceConfigure,
            GsvcExceptionHandler gsvcExceptionHandler, ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider) {
        return new ProxyExchangeHandler(headersFiltersProvider, gsvcExceptionHandler, serviceConfigure);
    }

    @Configuration(proxyBeanMethods = false)
    @RequiredArgsConstructor
    static class GsvcServer implements SmartLifecycle {

        private final ApplicationContext applicationContext;

        private final GatewayServiceConfigure gatewayServiceConfigure;

        private final Counter counter;

        private volatile boolean running = false;

        @Override
        public void start() {
            val services = GatewayServiceRegistry.DECLARED_SERVICES;
            val globalPlugins = gatewayServiceConfigure.getGlobalPlugins();
            for (Map.Entry<Class<?>, ServiceInfo> svc : services.entrySet()) {
                val service = svc.getValue();
                val cfgName = service.getCfgName();
                val interfaceName = service.getClazz();
                val bean = applicationContext.getBean(interfaceName);
                GatewayServiceRegistry.setBean(interfaceName, bean, service.isLocal());

                // setup plugin
                val methods = GatewayServiceRegistry.getDeclaredServiceMethods(interfaceName);
                for (Map.Entry<String, ServiceMethod> mv : methods.entrySet()) {
                    val method = mv.getValue();
                    // global is always enabled
                    for (IPlugin plugin : globalPlugins) {
                        method.registerPlugin(plugin);
                    }
                    // method > service > default. plugin prefixed '-' will be disabled.
                    val plugins = gatewayServiceConfigure.getPlugins(cfgName, method.getDmName(), !service.isLocal());
                    for (String plugin : plugins) {
                        try {
                            method.registerPlugin(applicationContext.getBean(plugin, IPlugin.class));
                        }
                        catch (Exception e) {
                            log.warn("Plugin {} not found for {}.{}!", plugin, cfgName, method.getDmName());
                        }
                    }
                }
            }
            running = true;
        }

        @Override
        public void stop() {
            counter.shutdown();
        }

        @Override
        public boolean isRunning() {
            return running;
        }

    }

}
