package com.apzda.cloud.gsvc.autoconfigure;

import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpClient;
import com.apzda.cloud.adapter.servlet.CommonFilter;
import com.apzda.cloud.adapter.servlet.callback.RequestOriginParser;
import com.apzda.cloud.adapter.servlet.callback.UrlBlockHandler;
import com.apzda.cloud.adapter.servlet.callback.UrlCleaner;
import com.apzda.cloud.adapter.servlet.callback.WebCallbackManager;
import com.apzda.cloud.gsvc.plugin.SentinelPlugin;
import com.apzda.cloud.gsvc.sentinel.DefaultRequestOriginParser;
import com.apzda.cloud.gsvc.sentinel.DefaultUrlBlockHandler;
import com.apzda.cloud.gsvc.sentinel.DefaultUrlCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author fengz
 */
@AutoConfiguration(before = ApzdaGsvcAutoConfiguration.class)
@ConditionalOnClass(SimpleHttpClient.class)
@ConditionalOnProperty("csp.sentinel.dashboard.server")
@Import({ SentinelGrpClientConfiguration.class, SentinelGrpcServerConfiguration.class })
@Slf4j
public class SentinelAutoConfiguration {

    @Bean
    SentinelPlugin gsvcSentinelPlugin() {
        return new SentinelPlugin();
    }

    @Bean
    public FilterRegistrationBean<CommonFilter> sentinelFilterRegistration(UrlBlockHandler urlBlockHandler,
            RequestOriginParser originParser, UrlCleaner urlCleaner) {
        WebCallbackManager.setUrlBlockHandler(urlBlockHandler);
        WebCallbackManager.setRequestOriginParser(originParser);
        WebCallbackManager.setUrlCleaner(urlCleaner);

        FilterRegistrationBean<CommonFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CommonFilter());
        registration.addUrlPatterns("/*");
        registration.setName("gsvcSentinelFilter");
        registration.setOrder(-2147483647);

        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    UrlBlockHandler defaultUrlBlockHandler() {
        return new DefaultUrlBlockHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestOriginParser defaultRequestOriginParser() {
        return new DefaultRequestOriginParser();
    }

    @Bean
    @ConditionalOnMissingBean
    UrlCleaner defaultUrlCleaner() {
        return new DefaultUrlCleaner();
    }

}
