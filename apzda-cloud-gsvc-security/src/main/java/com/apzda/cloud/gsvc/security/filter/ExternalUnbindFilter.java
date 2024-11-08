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
import com.apzda.cloud.gsvc.security.exception.ExternalUnbindException;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class ExternalUnbindFilter extends AbstractAuthenticatedFilter {

    public ExternalUnbindFilter(Set<RequestMatcher> excludes, SecurityConfigProperties properties) {
        super(excludes, properties);
    }

    @Override
    protected boolean doFilter(@Nonnull Authentication authentication, @Nonnull UserDetails userDetails) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            val jwtToken = jwtAuthenticationToken.getJwtToken();
            if (StringUtils.isBlank(jwtToken.getUid()) || "0".equals(jwtToken.getUid())) {
                throw new ExternalUnbindException();
            }
        }
        return true;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

}
