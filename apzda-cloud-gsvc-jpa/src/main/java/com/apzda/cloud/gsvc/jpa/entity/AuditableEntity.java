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
package com.apzda.cloud.gsvc.jpa.entity;

import com.apzda.cloud.gsvc.jpa.AuditingEntityListener;
import com.apzda.cloud.gsvc.model.Auditable;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.2
 * @since 3.4.2
 **/

@Getter
@Setter
@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class })
public abstract class AuditableEntity<ID, U, T> implements Auditable<ID, U, T> {

    private U createdBy;

    private T createdAt;

    private U updatedBy;

    private T updatedAt;

}
