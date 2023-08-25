package com.apzda.cloud.gsvc.autoconfigure;

import cn.dev33.satoken.filter.SaFilter;
import cn.dev33.satoken.filter.SaServletFilter;
import com.apzda.cloud.gsvc.core.SaTokenExtendProperties;
import com.apzda.cloud.gsvc.error.GsvcErrorAttributes;
import com.apzda.cloud.gsvc.error.GsvcErrorController;
import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.filter.GsvcFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author ninggf
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class ApzdaGsvcWebConfig {
    private final ServerProperties serverProperties;

    public ApzdaGsvcWebConfig(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Bean
    GsvcExceptionHandler gsvcExceptionHandler(SaTokenExtendProperties properties) {
        return new GsvcExceptionHandler(properties);
    }

    //@Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    ErrorAttributes gsvcErrorAttributes(GsvcExceptionHandler handler) {
        return new GsvcErrorAttributes(handler);
    }

    //@Bean
    @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
    public BasicErrorController basicErrorController(ErrorAttributes errorAttributes,
                                                     ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        return new GsvcErrorController(errorAttributes, this.serverProperties.getError(),
                                       errorViewResolvers.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    WebClient webClient(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        return WebClient.builder().filter(lbFunction).build();
    }

    @Bean
    @Order
    SaFilter gsvcDefaultSaReactorFilter() {
        return new SaServletFilter().addExclude("/**");
    }

    @Bean
    GsvcFilter gsvcFilter() {
        return new GsvcFilter();
    }
}
