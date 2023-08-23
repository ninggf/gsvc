package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.ResponseUtils;
import com.apzda.cloud.gsvc.core.*;
import com.apzda.cloud.gsvc.filter.ExporterGatewayFilterFactory;
import com.apzda.cloud.gsvc.filter.GatewayServiceFilter;
import com.apzda.cloud.gsvc.filter.ReferenceServiceFilter;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@AutoConfiguration(before = {GatewayAutoConfiguration.class})
@EnableConfigurationProperties({
    ServiceConfigurationProperties.class,
    SaTokenExtendProperties.class
})
@Import({WebFluxConfig.class})
@ConditionalOnClass(ReactiveResilience4JCircuitBreakerFactory.class)
@Slf4j
@RequiredArgsConstructor
public class ApzdaGsvcAutoConfiguration implements SmartLifecycle, InitializingBean {
    private final ServiceConfigurationProperties properties;
    private final ApplicationContext applicationContext;
    private volatile boolean isRunning = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (!StringUtils.hasText(appName)) {
            throw new IllegalStateException("'spring.application.name' must be not empty!");
        }
        ResponseUtils.config(properties);
    }

    @Override
    public void start() {
        val appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        val service = properties.getService();
        if (service != null) {
            val interfaceName = service.getInterfaceName();
            val name = service.getName();
            val bean = applicationContext.getBean(interfaceName);
            GatewayServiceRegistry.register(appName, name, -1, bean, interfaceName);
        }

        val references = properties.getReference();
        for (int i = 0; i < references.size(); i++) {
            val rc = references.get(i);
            val name = rc.getName();
            val interfaceName = rc.getInterfaceName();
            val bean = applicationContext.getBean(interfaceName);
            if (GatewayServiceRegistry.isLocalService(i)) {
                GatewayServiceRegistry.register(appName, name, i, bean, interfaceName);
            } else {
                GatewayServiceRegistry.register(rc.getApp(), name, i, bean, interfaceName);
            }
        }
        isRunning = true;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return 100;
    }


    @Bean
    GatewayServiceConfigure gatewayServiceConfigure(
        HttpClientProperties httpClientProperties,
        ServiceConfigurationProperties properties
    ) {
        return new GatewayServiceConfigure(httpClientProperties, properties);
    }

    @Bean
    public static GatewayServiceBeanFactoryPostProcessor serviceReferenceRegistrar() {
        return new GatewayServiceBeanFactoryPostProcessor();
    }

    @Bean
    public ExporterGatewayFilterFactory serviceGatewayFilterFactory() {
        return new ExporterGatewayFilterFactory();
    }

    @Bean
    public GlobalFilter openFeignFilter(ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
                                        ApplicationContext applicationContext) {
        return new GatewayServiceFilter(headersFiltersProvider, applicationContext);
    }

    @Bean
    public GlobalFilter referenceServiceFilter(SaTokenExtendProperties properties) {
        return new ReferenceServiceFilter(properties);
    }

    @Bean
    public GenericConverter toKvConverter() {
        return new GenericConverter() {
            @Override
            public Set<ConvertiblePair> getConvertibleTypes() {
                val convertiblePair = new ConvertiblePair(String.class, ExporterGatewayFilterFactory.KeyValue.class);
                return Collections.singleton(convertiblePair);
            }

            @Override
            public Object convert(Object source,
                                  @NonNull TypeDescriptor sourceType,
                                  @NonNull TypeDescriptor targetType) {
                val kv = Splitter.on(CharMatcher.anyOf("=:"))
                                 .omitEmptyStrings()
                                 .trimResults()
                                 .splitToList(source.toString());
                return new ExporterGatewayFilterFactory.KeyValue(kv.get(0), kv.get(1));
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Caffeine.class)
    @Slf4j
    static class CacheConfig {
        @Bean
        @ConditionalOnMissingBean
        public Caffeine caffeineConfig() {
            log.debug("Creating Caffeine cache backend");
            return Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES);
        }

        @Bean
        @ConditionalOnMissingBean
        public CacheManager cacheManager(Caffeine caffeine) {
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            caffeineCacheManager.setCaffeine(caffeine);
            return caffeineCacheManager;
        }
    }
}
