/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.gsvc.security.filter;

import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.exception.MfaException;
import com.apzda.cloud.gsvc.security.mfa.MfaStatus;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;

/**
 * 这个过滤器目前使用暴力方式解决多因素认证问题.
 *
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class MfaAuthenticationFilter extends AbstractAuthenticatedFilter {

    public MfaAuthenticationFilter(Set<RequestMatcher> excludes, SecurityConfigProperties properties) {
        super(excludes, properties);
    }

    @Override
    protected boolean doFilter(@Nonnull Authentication authentication, @Nonnull UserDetails userDetails) {
        if (properties.isAccountLockedEnabled() && !userDetails.isAccountNonLocked()) {
            return true;
        }

        if (properties.isCredentialsExpiredEnabled() && !userDetails.isCredentialsNonExpired()) {
            return true;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            val jwtToken = jwtAuthenticationToken.getJwtToken();
            val mfaStatus = jwtToken.getMfa();
            log.trace("Mfa Status of '{}' is: {}", authentication.getName(), mfaStatus);

            if (mfaStatus == null) {
                return true;
            }

            switch (mfaStatus) {
                case MfaStatus.UNSET -> throw MfaException.UNSET;
                case MfaStatus.PENDING -> throw MfaException.NOT_VERIFIED;
            }
        }

        return true;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 40;
    }

}
