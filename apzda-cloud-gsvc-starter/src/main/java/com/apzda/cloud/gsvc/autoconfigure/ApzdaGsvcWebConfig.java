package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.core.SaTokenExtendProperties;
import com.apzda.cloud.gsvc.core.ServiceConfigurationProperties;
import com.apzda.cloud.gsvc.error.GsvcErrorAttributes;
import com.apzda.cloud.gsvc.error.GsvcErrorController;
import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.filter.GsvcFilter;
import com.apzda.cloud.gsvc.gtw.filter.LoginFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

/**
 * @author ninggf
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ServiceConfigurationProperties.class, SaTokenExtendProperties.class })
public class ApzdaGsvcWebConfig {

    private final ServerProperties serverProperties;

    public ApzdaGsvcWebConfig(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Bean("login")
    @ConditionalOnMissingBean(name = "login")
    HandlerFilterFunction<ServerResponse, ServerResponse> login() {
        return new LoginFilter();
    }

    @Bean
    GsvcExceptionHandler gsvcExceptionHandler(SaTokenExtendProperties properties,
            ObjectProvider<List<HttpMessageConverter<?>>> httpMessageConverters) {
        return new GsvcExceptionHandler(properties, httpMessageConverters);
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    ErrorAttributes gsvcErrorAttributes(GsvcExceptionHandler handler) {
        return new GsvcErrorAttributes(handler);
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
    public BasicErrorController basicErrorController(ErrorAttributes errorAttributes, GsvcExceptionHandler handler,
            ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        return new GsvcErrorController(errorAttributes, this.serverProperties.getError(), handler,
                errorViewResolvers.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    WebClient webClient(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        return WebClient.builder().filter(lbFunction).build();
    }

    @Bean
    GsvcFilter gsvcFilter() {
        return new GsvcFilter();
    }

}
