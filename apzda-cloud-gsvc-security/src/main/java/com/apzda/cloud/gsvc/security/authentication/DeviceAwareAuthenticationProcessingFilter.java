package com.apzda.cloud.gsvc.security.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author fengz
 */
public abstract class DeviceAwareAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter
        implements Ordered {

    protected AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new DeviceAuthenticationDetailsSource();

    protected DeviceAwareAuthenticationProcessingFilter(String defaultFilterProcessesUrl,
            AuthenticationManager authenticationManager) {
        super(defaultFilterProcessesUrl, authenticationManager);
    }

    protected DeviceAwareAuthenticationProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher,
            AuthenticationManager authenticationManager) {
        super(requiresAuthenticationRequestMatcher, authenticationManager);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    protected void setDetails(HttpServletRequest request, AbstractAuthenticationToken authRequest) {
        if (authRequest.getDetails() == null) {
            authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
        }
    }

}
