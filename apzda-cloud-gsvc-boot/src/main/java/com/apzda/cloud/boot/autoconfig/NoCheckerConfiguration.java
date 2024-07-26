package com.apzda.cloud.boot.autoconfig;

import com.apzda.cloud.boot.security.AclChecker;
import com.apzda.cloud.boot.security.NoChecker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)

class NoCheckerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AclChecker aclPermissionChecker() {
        return new NoChecker();
    }

}
