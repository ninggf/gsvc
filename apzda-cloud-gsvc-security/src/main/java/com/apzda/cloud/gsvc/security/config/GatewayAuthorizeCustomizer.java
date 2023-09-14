package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.gtw.RouteMeta;
import com.apzda.cloud.gsvc.security.AuthorizeCustomizer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import java.util.Map;

/**
 * @author fengz
 */
@Slf4j
class GatewayAuthorizeCustomizer implements AuthorizeCustomizer {

    @Override
    public void customize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {

        if (!GatewayServiceRegistry.AUTHED_ROUTES.isEmpty()) {
            log.debug("Found Authed Pages: {}", GatewayServiceRegistry.AUTHED_ROUTES);
            for (Map.Entry<String, RouteMeta> kv : GatewayServiceRegistry.AUTHED_ROUTES.entrySet()) {
                val path = kv.getKey();
                val meta = kv.getValue();
                val access = meta.getAccess();
                if (StringUtils.isNotBlank(access)) {
                    authorize.requestMatchers(path).access(new WebExpressionAuthorizationManager(access));
                }
                else {
                    authorize.requestMatchers(path).authenticated();
                }
            }
        }

    }

}
