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
package com.apzda.cloud.gsvc.domain;

import com.apzda.cloud.gsvc.model.Auditable;
import com.apzda.cloud.gsvc.model.SoftDeletable;
import com.apzda.cloud.gsvc.model.Tenantable;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLRestriction;

import static com.apzda.cloud.gsvc.domain.SnowflakeIdGenerator.NAME;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@EntityListeners({ AuditingEntityListener.class })
@GenericGenerator(name = NAME, type = SnowflakeIdGenerator.class)
@MappedSuperclass
@Getter
@Setter
@SQLRestriction("deleted = false")
public abstract class SimpleEntity implements Auditable<Long, String, Long>, Tenantable<Long>, SoftDeletable {

    public static final String SNOWFLAKE = NAME;

    private String createdBy;

    private Long createdAt;

    private String updatedBy;

    private Long updatedAt;

    private Long tenantId;

    private boolean deleted;

}
