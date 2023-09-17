package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.security.TokenManager;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
class DefaultAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;

    private final UserDetailsMetaRepository userDetailsMetaRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenManager tokenManager;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.warn("You are using a demo AuthenticationProvider, please use a real one!!!");
        val credentials = authentication.getCredentials();
        val username = authentication.getPrincipal();
        val userDetails = userDetailsService.loadUserByUsername((String) username);
        val password = userDetails.getPassword();

        if (passwordEncoder.matches((CharSequence) credentials, password)) {
            return JwtAuthenticationToken.authenticated(userDetailsMetaRepository.create(userDetails), password);
        }

        throw new UsernameNotFoundException(authentication.getName());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
