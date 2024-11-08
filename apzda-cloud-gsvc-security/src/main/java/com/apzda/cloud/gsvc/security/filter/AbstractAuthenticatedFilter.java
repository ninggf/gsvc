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

import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractAuthenticatedFilter extends OncePerRequestFilter implements Ordered {

    public static final String ACCOUNT_LOCKED_FILTER = "accountLockedFilter";

    public static final String UNBIND_CHECK_FILTER = "externalUnbindFilter";

    public static final String MFA_FILTER = "mfaAuthenticationFilter";

    public static final String CREDENTIALS_FILTER = "credentialsExpiredFilter";

    private final Set<RequestMatcher> excludes;

    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy strategy) {
        Assert.notNull(strategy, "securityContextHolderStrategy cannot be null");
        securityContextHolderStrategy = strategy;
    }

    @Nullable
    protected Authentication getAuthentication() {
        return securityContextHolderStrategy.getContext().getAuthentication();
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        if (excludes.stream().anyMatch((m) -> m.matches(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        val authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (doFilter(authentication, (UserDetails) authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
        }
        else {
            throw new GsvcException(ServiceError.SERVICE_ERROR);
        }
    }

    protected abstract boolean doFilter(@Nonnull Authentication authentication, @Nonnull UserDetails userDetails);

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
