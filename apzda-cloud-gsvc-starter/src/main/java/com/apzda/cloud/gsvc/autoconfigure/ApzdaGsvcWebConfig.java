package com.apzda.cloud.gsvc.autoconfigure;

import cn.dev33.satoken.filter.SaFilter;
import cn.dev33.satoken.filter.SaServletFilter;
import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author ninggf
 */
@Configuration(proxyBeanMethods = false)
public class ApzdaGsvcWebConfig {

    @Bean
    public GsvcExceptionHandler gsvcExceptionHandler() {
        return new GsvcExceptionHandler();
    }

    @Bean
    @Order
    public SaFilter gsvcDefaultSaReactorFilter() {
        return new SaServletFilter().addExclude("/**");
    }
}
