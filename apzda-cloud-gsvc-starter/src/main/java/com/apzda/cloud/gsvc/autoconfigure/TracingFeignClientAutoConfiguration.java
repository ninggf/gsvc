package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.tracing.TracingFeignRequestInterceptor;
import feign.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Client.class })
class TracingFeignClientAutoConfiguration {

    @Bean
    TracingFeignRequestInterceptor tracingFeignRequestInterceptor() {
        return new TracingFeignRequestInterceptor();
    }

}
