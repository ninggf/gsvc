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
package com.apzda.cloud.gsvc.jpa;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.model.Auditable;
import com.apzda.cloud.gsvc.model.Tenantable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ResolvableType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.2
 * @since 3.4.2
 **/
public class AuditingEntityListener {

    @PrePersist
    @PreUpdate
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void fillMetaData(Object o) {
        if (o instanceof Auditable entity) {
            val userId = CurrentUserProvider.getCurrentUser().getId();

            val resolvableType = ResolvableType.forClass(Auditable.class, o.getClass());

            val userIdClz = resolvableType.getGeneric(1).resolve();

            Object uid;
            if (userIdClz == null) {
                uid = null;
            }
            else if (Long.class.isAssignableFrom(userIdClz)) {
                uid = Long.parseLong(userId);
            }
            else if (Integer.class.isAssignableFrom(userIdClz)) {
                uid = Integer.parseInt(userId);
            }
            else if (StringUtils.isNotBlank(userId)) {
                uid = userId;
            }
            else {
                uid = null;
            }

            if (uid != null) {
                if (Objects.isNull(entity.getCreatedBy())) {
                    entity.setCreatedBy(uid);
                }
                entity.setUpdatedBy(uid);
            }
            val timeType = resolvableType.getGeneric(2).resolve();

            Object current;
            if (timeType == null || Long.class.isAssignableFrom(timeType)) {
                current = System.currentTimeMillis();
            }
            else if (Date.class.isAssignableFrom(timeType)) {
                current = new Date();
            }
            else if (LocalDate.class.isAssignableFrom(timeType)) {
                current = LocalDate.now();
            }
            else if (LocalDateTime.class.isAssignableFrom(timeType)) {
                current = LocalDateTime.now();
            }
            else {
                current = DateUtil.now();
            }

            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(current);
            }

            entity.setUpdatedAt(current);
        }

        if (o instanceof Tenantable<?> tenantable) {
            if (Objects.isNull(tenantable.getTenantId())) {
                tenantable.setTenantId(TenantManager.tenantId());
            }
        }
    }

}
