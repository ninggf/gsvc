package com.apzda.cloud.gsvc.security.authorization;

import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * @author fengz
 */
public interface AuthorizeCustomizer extends Ordered {

    void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize);

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
