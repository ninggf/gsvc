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
package com.apzda.cloud.boot.controller;

import com.apzda.cloud.boot.dict.Dictionary;
import com.apzda.cloud.gsvc.domain.PagerUtils;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class BaseCrudController<T, S extends IService<T>> implements ApplicationContextAware {

    protected ApplicationContext context;

    protected Class<?> entityClass;

    protected S serviceImpl;

    @Override
    @SuppressWarnings("unchecked")
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
        val resolvableType = ResolvableType.forClass(BaseCrudController.class, this.getClass());
        this.entityClass = resolvableType.getGeneric(0).resolve();
        val entityServiceClz = resolvableType.getGeneric(1).resolve();
        assert entityServiceClz != null;
        serviceImpl = (S) context.getBean(entityServiceClz);
    }

    @PostMapping("/list")
    @SuppressWarnings("unchecked")
    @Dictionary
    public Response<IPage<T>> list(@RequestBody T entity, GsvcExt.Pager pager) {
        val query = Wrappers.lambdaQuery(entity);
        val page = (IPage<T>) PagerUtils.of(pager, this.entityClass);
        return Response.success(serviceImpl.page(page, query));
    }

}
