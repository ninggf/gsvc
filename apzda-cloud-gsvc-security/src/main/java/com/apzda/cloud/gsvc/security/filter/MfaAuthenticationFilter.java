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

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.mfa.MfaException;
import com.apzda.cloud.gsvc.security.mfa.MfaStatus;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 这个过滤器目前使用暴力方式解决多因素认证问题.
 *
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class MfaAuthenticationFilter extends OncePerRequestFilter {
    private final Set<RequestMatcher> excludes;
    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws
                                                                      ServletException,
                                                                      IOException {
        if (excludes.stream().anyMatch((m) -> m.matches(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        val authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authentication.getPrincipal() instanceof UserDetailsMeta userDetailsMeta) {
            val mfaStatus = userDetailsMeta.get(UserDetailsMeta.MFA_STATUS_KEY, authentication, MfaStatus.DISABLED);

            log.trace("Mfa Status of '{}' is: {}", userDetailsMeta.getUsername(), mfaStatus);

            switch (mfaStatus) {
                case MfaStatus.UNSET -> throw MfaException.UNSET;
                case MfaStatus.PENDING -> throw MfaException.NOT_VERIFIED;
            }
        }

        filterChain.doFilter(request, response);
    }

    public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
        Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

    private Authentication getAuthentication() {
        return this.securityContextHolderStrategy.getContext().getAuthentication();
    }
}
