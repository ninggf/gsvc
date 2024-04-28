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

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class AsteriskPermissionEvaluator implements PermissionEvaluator {

    private final ObjectProvider<PermissionChecker> checkerProvider;

    private final static LoadingCache<String, Predicate<String>> PERMISSION_PATTERNS_CACHE = CacheBuilder.newBuilder()
        .maximumSize(500)
        .build(new CacheLoader<>() {
            @Override
            @NonNull
            public Predicate<String> load(@NonNull String key) {
                boolean suffix = false;
                if (org.apache.commons.lang3.StringUtils.endsWith(key, ".*")) {
                    key = key.substring(0, key.length() - 2);
                    suffix = true;
                }

                key = Pattern.compile("([a-z0-9_-]+)(,[a-z0-9_-]+)+", Pattern.CASE_INSENSITIVE)
                    .matcher(key)
                    .replaceAll((mr) -> "(" + mr.group(0).replace(',', '|') + ")");

                val strings = new ArrayList<>(Splitter.on(":").trimResults().omitEmptyStrings().splitToList(key));
                if (strings.size() == 1) {
                    strings.add(0, "*");
                }

                var pattern = String.join(":", strings).replace(".", "\\.").replace("*", "(.+?)");
                if (suffix) {
                    pattern += ".*";
                }
                return Pattern.compile("^" + pattern + "$").asMatchPredicate();
            }
        });

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if ((authentication == null) || !(permission instanceof String)) {
            return false;
        }
        if (targetDomainObject instanceof Boolean allow) {
            return allow;
        }
        if (targetDomainObject != null) {
            val aClass = targetDomainObject.getClass();
            val checkers = checkerProvider.orderedStream().toList();

            for (PermissionChecker checker : checkers) {
                if (checker.supports(aClass)) {
                    val allowed = checker.check(authentication, targetDomainObject, (String) permission);
                    if (allowed != null) {
                        return allowed;
                    }
                }
            }

            return hasPrivilege(authentication, targetDomainObject.toString(), (String) permission);
        }
        else {
            return hasPrivilege(authentication, null, (String) permission);
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
            Object permission) {
        if ((authentication == null) || (targetId == null) || (targetType == null) || !(permission instanceof String)) {
            return false;
        }

        val checkers = checkerProvider.orderedStream().toList();
        for (PermissionChecker checker : checkers) {
            if (checker.supports(targetType)) {
                val allowed = checker.check(authentication, targetId, targetType, (String) permission);
                if (allowed != null) {
                    return allowed;
                }
            }
        }
        return hasPrivilege(authentication, targetType + "/" + targetId, (String) permission);
    }

    private boolean hasPrivilege(Authentication auth, String id, String permission) {
        var authority = permission;
        if (StringUtils.hasText(id)) {
            authority += "." + id;
        }

        for (GrantedAuthority grantedAuthority : auth.getAuthorities()) {
            val granted = grantedAuthority.getAuthority();
            try {
                val pattern = PERMISSION_PATTERNS_CACHE.get(granted);
                if (pattern.test(authority)) {
                    if (log.isTraceEnabled()) {
                        log.trace("{} matched {}", authority, granted);
                    }
                    return true;
                }
            }
            catch (Exception e) {
                log.warn("Cannot parse authority({}) to ant pattern: {}", granted, e.getMessage());
            }
        }

        return false;
    }

}
