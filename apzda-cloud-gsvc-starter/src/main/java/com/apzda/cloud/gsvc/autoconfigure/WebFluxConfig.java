package com.apzda.cloud.gsvc.autoconfigure;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * @author ninggf
 */
@Configuration(proxyBeanMethods = false)
public class WebFluxConfig implements WebFluxConfigurer {

    @Bean
    public GsvcExceptionHandler gsvcExceptionHandler() {
        return new GsvcExceptionHandler();
    }

    @Bean
    public SaReactorFilter gsvcDefaultSaReactorFilter() {
        return new SaReactorFilter().addExclude("/**");
    }
}
