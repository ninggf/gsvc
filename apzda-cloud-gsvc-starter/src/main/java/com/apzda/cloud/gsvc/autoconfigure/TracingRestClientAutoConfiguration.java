package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.tracing.TracingRestTemplateInterceptor;
import com.apzda.cloud.gsvc.tracing.TracingRestTemplateInterceptorAfterPropertiesSet;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
class TracingRestClientAutoConfiguration {

    @Bean
    @ConditionalOnBean(ObservationRegistry.class)
    static TracingRestTemplateInterceptor tracingRestTemplateInterceptor() {
        return new TracingRestTemplateInterceptor();
    }

    @Bean
    @ConditionalOnBean(TracingRestTemplateInterceptor.class)
    TracingRestTemplateInterceptorAfterPropertiesSet tracingRestTemplateInterceptorAfterPropertiesSet(
            TracingRestTemplateInterceptor tracingRestTemplateInterceptor,
            ObjectProvider<RestTemplate> objectProvider) {
        return new TracingRestTemplateInterceptorAfterPropertiesSet(tracingRestTemplateInterceptor, objectProvider);
    }

}
