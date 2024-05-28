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
package com.apzda.cloud.gsvc.security.utils;

import lombok.val;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class SecurityUtils {

    private static DefaultSecurityExpressionHandler handler;

    public static SecurityExpressionRoot security() {
        return handler.create();
    }

    public static class DefaultSecurityExpressionHandler {

        private final PermissionEvaluator permissionEvaluator;

        private final GrantedAuthorityDefaults grantedAuthorityDefaults;

        private final RoleHierarchy roleHierarchy;

        private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

        public DefaultSecurityExpressionHandler(PermissionEvaluator permissionEvaluator,
                GrantedAuthorityDefaults grantedAuthorityDefaults, RoleHierarchy roleHierarchy) {
            this.permissionEvaluator = permissionEvaluator;
            this.grantedAuthorityDefaults = grantedAuthorityDefaults;
            this.roleHierarchy = roleHierarchy;
            SecurityUtils.handler = this;
        }

        SecurityExpressionRoot create() {
            val root = new GsvcSecurityExpressionRoot(() -> SecurityContextHolder.getContext().getAuthentication());
            root.setDefaultRolePrefix(grantedAuthorityDefaults.getRolePrefix());
            root.setPermissionEvaluator(permissionEvaluator);
            root.setRoleHierarchy(roleHierarchy);
            root.setTrustResolver(trustResolver);
            return root;
        }

    }

    static class GsvcSecurityExpressionRoot extends SecurityExpressionRoot {

        public GsvcSecurityExpressionRoot(Supplier<Authentication> authentication) {
            super(authentication);
        }

    }

}
