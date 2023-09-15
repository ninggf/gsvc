package com.apzda.cloud.gsvc.security;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.security.token.AuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

/**
 * @author fengz windywany@gmail.com
 */
public interface TokenManager {

    AuthenticationToken restore(HttpServletRequest request);

    default void saveToken(Authentication authentication, HttpServletRequest request) {

    }

    JwtToken createJwtToken(AuthenticationToken authentication);

    JwtToken refresh(JwtToken token, AuthenticationToken authentication);

    default void verify(JwtToken token, AuthenticationToken authentication) throws SessionAuthenticationException {
        if (token != null) {
            val expireAt = token.getExpireAt();
            if (expireAt != null) {
                // expire - 60 > current
                if (expireAt.before(DateUtil.date().offset(DateField.SECOND, 30))) {
                    return;
                }
            }
        }

        throw new SessionAuthenticationException("Login Session expired");
    }

    default void remove(JwtToken token) {
    }

}
