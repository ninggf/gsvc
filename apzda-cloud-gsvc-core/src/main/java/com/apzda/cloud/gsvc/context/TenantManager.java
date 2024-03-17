/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.context;

import lombok.val;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class TenantManager<T> implements InitializingBean {

    private static final Object[] SYS_TENANT_IDS = new Object[]{null};

    private static TenantManager<?> tenantManager;
    private static Class<?> fieldType;

    @Override
    public void afterPropertiesSet() throws Exception {
        tenantManager = this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> T[] tenantIds() {
        if (tenantManager != null) {
            val tenantIds = tenantManager.getTenantIds();
            if (tenantIds.length > 0) {
                return (T[]) tenantIds;
            }
        }
        return (T[]) SYS_TENANT_IDS;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T tenantId() {
        if (Objects.isNull(tenantIds()[0])) {
            return null;
        }
        return (T) tenantIds()[0];
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> T tenantId(@NonNull T defaultTenantId) {
        Assert.notNull(defaultTenantId, "Default tenant ID cannot be null");
        if (Objects.isNull(tenantIds()[0])) {
            return defaultTenantId;
        }
        return (T) tenantIds()[0];
    }

    @NonNull
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    public boolean disableTenantPlugin() {
        return false;
    }

    @NonNull
    protected abstract T[] getTenantIds();

    public static Class<?> getIdType() {
        if (fieldType == null && tenantManager != null) {
            fieldType = ResolvableType.forClass(TenantManager.class, tenantManager.getClass()).getGeneric(0).resolve();
        }
        return fieldType;
    }
}
