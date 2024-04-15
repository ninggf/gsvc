package com.apzda.cloud.gsvc.autoconfigure;

import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpClient;
import com.apzda.cloud.adapter.spring.SentinelWebInterceptor;
import com.apzda.cloud.adapter.spring.callback.BlockExceptionHandler;
import com.apzda.cloud.adapter.spring.callback.RequestOriginParser;
import com.apzda.cloud.adapter.spring.callback.UrlCleaner;
import com.apzda.cloud.adapter.spring.config.SentinelWebMvcConfig;
import com.apzda.cloud.gsvc.gtw.GatewayUrlCleaner;
import com.apzda.cloud.sentinel.callback.StandardRequestOriginParser;
import com.apzda.cloud.sentinel.callback.StandardUrlBlockHandler;
import com.apzda.cloud.sentinel.plugin.SentinelPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author fengz
 */
@AutoConfiguration(before = GsvcAutoConfiguration.class)
@ConditionalOnClass(SimpleHttpClient.class)
@ConditionalOnProperty("csp.sentinel.dashboard.server")
@Import({ SentinelGrpClientConfiguration.class, SentinelGrpcServerConfiguration.class })
@Slf4j
public class SentinelAutoConfiguration {

    @Bean
    SentinelPlugin gsvcSentinelPlugin() {
        log.trace("SentinelPlugin created");
        return new SentinelPlugin();
    }

    @Bean
    @ConditionalOnMissingBean
    BlockExceptionHandler blockExceptionHandler() {
        return new StandardUrlBlockHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestOriginParser requestOriginParser() {
        return new StandardRequestOriginParser();
    }

    @Bean
    @ConditionalOnMissingBean
    UrlCleaner urlCleaner() {
        return new GatewayUrlCleaner();
    }

    @Configuration(proxyBeanMethods = false)
    @RequiredArgsConstructor
    static class SentinelWebMvcConfigurer implements WebMvcConfigurer {

        private final BlockExceptionHandler blockExceptionHandler;

        private final RequestOriginParser requestOriginParser;

        private final UrlCleaner urlCleaner;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            val config = new SentinelWebMvcConfig();
            // Enable the HTTP method prefix.
            config.setHttpMethodSpecify(true);
            config.setWebContextUnify(true);
            config.setBlockExceptionHandler(blockExceptionHandler);
            config.setOriginParser(requestOriginParser);
            config.setUrlCleaner(urlCleaner);
            // Add to the interceptor list.
            val sentinelWebInterceptor = new SentinelWebInterceptor(config);
            registry.addInterceptor(sentinelWebInterceptor).addPathPatterns("/**");
            log.info("SentinelWebInterceptor installed for /**: {}", config);
        }

    }

}
