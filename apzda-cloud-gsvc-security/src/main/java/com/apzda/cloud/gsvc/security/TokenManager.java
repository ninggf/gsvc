package com.apzda.cloud.gsvc.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

/**
 * @author fengz windywany@gmail.com
 */
public interface TokenManager {

    Authentication restoreAuthentication(HttpServletRequest request);

    default void save(Authentication authentication, HttpServletRequest request) {
    }

    default void remove(Authentication authentication, HttpServletRequest request) {
    }

    JwtToken createJwtToken(Authentication authentication);

    JwtToken refreshAccessToken(JwtToken token, Authentication authentication);

    default void verify(@NonNull Authentication authentication) throws SessionAuthenticationException {
    }

    String createRefreshToken(String accessToken, Authentication authentication);

}
