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
package com.apzda.cloud.boot.domain;

import com.apzda.cloud.gsvc.model.IEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface DomainService<D extends Serializable, T extends IEntity<D>> extends IService<T> {

    default IPage<T> read(IPage<T> page, QueryWrapper<T> query) {
        return page(page, query);
    }

    default T read(Serializable id) {
        return getById(id);
    }

    default boolean delete(Serializable id) {
        return removeById(id);
    }

    default boolean update(D id, T entity) {
        entity.setId(id);
        return updateById(entity);
    }

    default T create(T entity) {
        if (save(entity)) {
            return entity;
        }
        return null;
    }

}
