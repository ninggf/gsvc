package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.security.AuthorizeCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * @author fengz
 */
@Slf4j
class GatewayAuthorizeCustomizer implements AuthorizeCustomizer {

    @Override
    public void customize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {

        if (!GatewayServiceRegistry.AUTHED_ROUTES.isEmpty()) {
            log.info("Found Authed Pages: {}", GatewayServiceRegistry.AUTHED_ROUTES);
            for (String path : GatewayServiceRegistry.AUTHED_ROUTES) {
                authorize.requestMatchers(path).authenticated();
            }
        }
    }

}
