package com.apzda.cloud.gsvc.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author fengz
 */
@Slf4j
class DefaultAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.warn("Please configure a AuthenticationProvider!!!");
        throw new UsernameNotFoundException(authentication.getName());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }

}
