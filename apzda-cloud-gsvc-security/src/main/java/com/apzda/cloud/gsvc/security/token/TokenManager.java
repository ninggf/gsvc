package com.apzda.cloud.gsvc.security.token;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

/**
 * @author fengz windywany@gmail.com
 */
public interface TokenManager {

    Authentication restoreAuthentication(HttpServletRequest request);

    Authentication restoreAuthentication(String accessToken);

    default void save(Authentication authentication, HttpServletRequest request) {
    }

    default void remove(Authentication authentication, HttpServletRequest request) {
    }

    JwtToken createJwtToken(Authentication authentication);

    JwtToken refreshAccessToken(@NonNull JwtToken jwtToken);

    default void verify(@NonNull Authentication authentication) throws SessionAuthenticationException {
    }

    @NonNull
    String createRefreshToken(@NonNull JwtToken jwtToken, @NonNull Authentication authentication);

}
