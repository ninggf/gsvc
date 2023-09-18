package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
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
        log.warn("[{}] You are using a demo AuthenticationProvider, please use a real one!!!",
                GsvcContextHolder.getRequestId());

        val credentials = authentication.getCredentials();
        val username = authentication.getPrincipal();
        val userDetails = userDetailsService.loadUserByUsername((String) username);
        val password = userDetails.getPassword();

        if (!userDetails.isEnabled()) {
            throw new DisabledException(String.format("%s is disabled", username));
        }
        if (!userDetails.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(String.format("%s's password is expired", username));
        }
        if (!userDetails.isAccountNonExpired()) {
            throw new AccountExpiredException(String.format("%s's account is expired", username));
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new LockedException(String.format("%s's account is expired", username));
        }

        if (passwordEncoder.matches((CharSequence) credentials, password)) {
            return JwtAuthenticationToken.authenticated(userDetailsMetaRepository.create(userDetails), password);
        }

        throw new BadCredentialsException(String.format("%s's password does not match", username));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
