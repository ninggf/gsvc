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
package com.apzda.cloud.gsvc.domain;

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class AutoMetaListener {

    @PrePersist
    @PreUpdate
    void fillMetaData(Object o) {
        if (o instanceof AuditedEntity auditedEntity) {
            val userId = CurrentUserProvider.getCurrentUser().getUid();
            val current = System.currentTimeMillis();
            if (StringUtils.isBlank(auditedEntity.getCreatedBy())) {
                auditedEntity.setCreatedBy(userId);
            }
            if (auditedEntity.getCreatedAt() == null) {
                auditedEntity.setCreatedAt(current);
            }
            auditedEntity.setUpdatedBy(userId);
            auditedEntity.setUpdatedAt(current);
        }

        if (o instanceof TenantedEntity tenantedEntity) {
            if (StringUtils.isBlank(tenantedEntity.getTenantId())) {
                tenantedEntity.setTenantId(TenantManager.tenantId());
            }
        }
    }

}
