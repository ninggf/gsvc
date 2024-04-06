/*
 * Copyright (C) 2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.gsvc.security.authorization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class AsteriskPermissionEvaluator implements PermissionEvaluator {

    private final ObjectProvider<List<PermissionChecker>> checkerProvider;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if ((authentication == null) || (targetDomainObject == null) || !(permission instanceof String)) {
            return false;
        }
        if (targetDomainObject instanceof Boolean allow) {
            return allow;
        }
        val aClass = targetDomainObject.getClass();
        val checkers = checkerProvider.getIfAvailable();
        if (!CollectionUtils.isEmpty(checkers)) {
            for (PermissionChecker checker : checkers) {
                if (checker.supports(aClass)) {
                    val allowed = checker.check(authentication, targetDomainObject, (String) permission);
                    if (allowed != null) {
                        return allowed;
                    }
                }
            }
        }

        return hasPrivilege(authentication, targetDomainObject.toString(), (String) permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
            Object permission) {
        if ((authentication == null) || (targetId == null) || (targetType == null) || !(permission instanceof String)) {
            return false;
        }

        val checkers = checkerProvider.getIfAvailable();
        if (!CollectionUtils.isEmpty(checkers)) {
            for (PermissionChecker checker : checkers) {
                if (checker.supports(targetType)) {
                    val allowed = checker.check(authentication, targetId, targetType, (String) permission);
                    if (allowed != null) {
                        return allowed;
                    }
                }
            }
        }
        return hasPrivilege(authentication, targetType + "/" + targetId, (String) permission);
    }

    private boolean hasPrivilege(Authentication auth, String id, String permission) {
        var authority = permission;
        if (StringUtils.hasText(id)) {
            authority += "/" + id;
        }
        for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
            val granted = grantedAuth.getAuthority();
            val asterisk = granted.contains("*");

            if (!asterisk && granted.equalsIgnoreCase(authority)) {
                if (log.isTraceEnabled()) {
                    log.trace("Permit for: {} equals {}", authority, granted);
                }
                return true;
            }

            if (asterisk && match(granted, authority)) {
                if (log.isTraceEnabled()) {
                    log.trace("Permit for: {} matched {}", authority, granted);
                }
                return true;
            }
        }

        return false;
    }

    private static boolean match(String authority, String toBeChecked) {
        val pattern = authority.replace("*", "(.+?)");
        val compile = Pattern.compile("^" + pattern + "$");
        return compile.asMatchPredicate().test(toBeChecked);
    }

}
