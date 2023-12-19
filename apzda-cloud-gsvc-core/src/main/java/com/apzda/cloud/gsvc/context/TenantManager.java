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

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class TenantManager implements InitializingBean {

    private static final String[] SYS_TENANT_IDS = new String[] { "0" };

    private static TenantManager tenantManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        tenantManager = this;
    }

    public static String[] tenantIds() {
        if (tenantManager != null) {
            val tenantIds = tenantManager.getTenantIds();
            if (tenantIds != null && tenantIds.length > 0) {
                return tenantIds;
            }
        }
        return SYS_TENANT_IDS;
    }

    public static String tenantId() {
        return tenantIds()[0];
    }

    protected abstract String[] getTenantIds();

}
