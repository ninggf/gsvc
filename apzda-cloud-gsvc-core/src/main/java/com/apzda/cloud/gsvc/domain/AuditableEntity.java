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
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface AuditableEntity {

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    Long getCreatedAt();

    void setCreatedAt(Long createdAt);

    String getUpdatedBy();

    void setUpdatedBy(String updatedBy);

    Long getUpdatedAt();

    void setUpdatedAt(Long updatedAt);

    boolean isDeleted();

    void setDeleted(boolean deleted);

    default void fillAuditInfo() {
        val user = CurrentUserProvider.getCurrentUser();
        fillAuditInfo(user.getUid());
    }

    default void fillAuditInfo(String userId) {
        val current = System.currentTimeMillis();
        if (StringUtils.isBlank(getCreatedBy())) {
            setCreatedBy(userId);
        }
        if (getCreatedAt() == null) {
            setCreatedAt(current);
        }
        setUpdatedBy(userId);
        setUpdatedAt(current);
    }

}
