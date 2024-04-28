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
package com.apzda.cloud.gsvc.security.authorization;

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.model.OwnerAware;
import com.apzda.cloud.gsvc.model.Tenantable;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class AuthorizationLogicCustomizer {

    private final PermissionEvaluator evaluator;

    public boolean isSa(MethodSecurityExpressionOperations operations) {
        return operations.hasRole("sa");
    }

    public boolean iCan(String authority) {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return evaluator.hasPermission(authentication, null, authority);
        }
        return false;
    }

    public boolean iCan(String authority, Object object) {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return evaluator.hasPermission(authentication, object, authority);
        }
        return false;
    }

    public boolean isMine(@Nullable OwnerAware<?> object) {
        if (object == null || object.getCreatedBy() == null) {
            return false;
        }
        val me = CurrentUserProvider.getCurrentUser();
        val uid = me.getUid();
        val owner = object.getCreatedBy().toString();

        return Objects.equals(uid, owner);
    }

    public boolean isMine(@Nullable String owner) {
        if (StringUtils.isBlank(owner)) {
            return false;
        }
        val me = CurrentUserProvider.getCurrentUser();
        val uid = me.getUid();
        return Objects.equals(uid, owner);
    }

    public boolean isTenanted(@Nullable Tenantable<?> object) {
        if (object == null || object.getTenantId() == null) {
            return false;
        }

        val tenantId = object.getTenantId().toString();
        return Arrays.stream(TenantManager.tenantIds())
            .filter(Objects::nonNull)
            .map(Object::toString)
            .anyMatch((id) -> id.equals(tenantId));
    }

    public boolean isTenanted(@Nullable String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return false;
        }

        return Arrays.stream(TenantManager.tenantIds())
            .filter(Objects::nonNull)
            .map(Object::toString)
            .anyMatch((id) -> id.equals(tenantId));
    }

}
