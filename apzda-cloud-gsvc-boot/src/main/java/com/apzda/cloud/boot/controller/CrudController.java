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
import com.apzda.cloud.boot.domain.DomainService;
import com.apzda.cloud.boot.query.QueryGenerator;
import com.apzda.cloud.boot.security.AclChecker;
import com.apzda.cloud.boot.validate.Group;
import com.apzda.cloud.gsvc.domain.PagerUtils;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.NotFoundError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.model.IEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class CrudController<D extends Serializable, T extends IEntity<D>, S extends DomainService<D, T>>
        implements ApplicationContextAware {

    private S serviceImpl;

    @Resource
    private AclChecker aclChecker;

    private String resourceId;

    private String createOp;

    private String readOp;

    private String updateOp;

    private String deleteOp;

    @Override
    @SuppressWarnings("unchecked")
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        val resolvableType = ResolvableType.forClass(CrudController.class, this.getClass());
        Class<T> entityClz = (Class<T>) resolvableType.getGeneric(1).resolve();
        Class<S> serviceClz = (Class<S>) resolvableType.getGeneric(2).resolve();
        assert entityClz != null && serviceClz != null;
        serviceImpl = (S) applicationContext.getBean(serviceClz);
        val resource = entityClz.getAnnotation(com.apzda.cloud.gsvc.acl.Resource.class);
        if (resource != null) {
            resourceId = StringUtils.defaultIfBlank(resource.id(), StringUtils.uncapitalize(entityClz.getSimpleName()));

            if (StringUtils.isNotBlank(resource.create())) {
                createOp = resource.create() + ":" + resourceId;
            }
            if (StringUtils.isNotBlank(resource.read())) {
                readOp = resource.read() + ":" + resourceId;
            }
            if (StringUtils.isNotBlank(resource.update())) {
                updateOp = resource.update() + ":" + resourceId;
            }
            if (StringUtils.isNotBlank(resource.delete())) {
                deleteOp = resource.delete() + ":" + resourceId;
            }
        }
    }

    /**
     * 分页查询列表.
     * @param entity 查询实体
     * @param pager 分页器
     * @param req 原生请求
     * @return 查询结果
     */
    @ResponseBody
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Dictionary
    public Response<IPage<T>> list(T entity, GsvcExt.Pager pager, HttpServletRequest req) {
        QueryWrapper<T> query = QueryGenerator.initQueryWrapper(entity, req.getParameterMap());
        IPage<T> page = PagerUtils.iPage(pager);

        if (StringUtils.isNotBlank(readOp)) {
            aclChecker.check(entity, readOp);
        }

        return Response.success(serviceImpl.read(page, query));
    }

    /**
     * 获取实体实例.
     * @param id 实体ID
     * @return 实体实例
     */
    @ResponseBody
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Dictionary
    public Response<T> read(@PathVariable D id) {
        val entity = serviceImpl.read(id);
        if (entity == null) {
            throw new GsvcException(new NotFoundError(resourceId, String.valueOf(id)));
        }
        if (StringUtils.isNotBlank(readOp)) {
            aclChecker.check(entity, readOp);
        }
        return Response.success(entity);
    }

    /**
     * 新增实体.
     * @param entity 要新增的实体实例.
     * @return 新增后的实体实例.
     */
    @ResponseBody
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Dictionary
    @Validated(Group.New.class)
    public Response<T> add(@RequestBody T entity) {
        if (StringUtils.isNotBlank(createOp)) {
            aclChecker.check(entity, createOp);
        }
        entity = serviceImpl.create(alter(entity));
        if (entity != null) {
            return Response.success(entity);
        }

        return Response.error(-995);
    }

    /**
     * 修改实体.
     * @param id 实体ID
     * @param entity 新的实体实例.
     * @return 修改后的实体实例.
     */
    @ResponseBody
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Dictionary
    @Validated(Group.Update.class)
    public Response<T> updateById(@PathVariable D id, @RequestBody T entity) {
        val o = serviceImpl.read(id);
        if (o == null) {
            throw new GsvcException(new NotFoundError(resourceId, String.valueOf(id)));
        }
        if (StringUtils.isNotBlank(updateOp)) {
            aclChecker.check(entity, updateOp);
        }
        if (serviceImpl.update(id, alter(o, entity))) {
            return Response.success(serviceImpl.read(id));
        }

        return Response.error(-995);
    }

    /**
     * 删除实体.
     * @param id 实体ID.
     * @return 被删除的实体.
     */
    @ResponseBody
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Dictionary
    public Response<T> deleteById(@PathVariable D id) {
        val o = serviceImpl.read(id);
        if (o == null) {
            throw new GsvcException(new NotFoundError(resourceId, String.valueOf(id)));
        }

        if (StringUtils.isNotBlank(deleteOp)) {
            aclChecker.check(o, deleteOp);
        }

        if (serviceImpl.delete(id)) {
            return Response.success(o);
        }

        return Response.error(-995);
    }

    /**
     * 批量删除实体.
     * @param ids 要删除的实体ID列表
     * @return 被删除的实体列表
     */
    @ResponseBody
    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Dictionary
    public Response<List<T>> delete(@RequestBody List<D> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Response.success(Collections.emptyList());
        }

        val entities = serviceImpl.listByIds(ids);
        if (CollectionUtils.isEmpty(entities)) {
            return Response.success(Collections.emptyList());
        }

        if (StringUtils.isNotBlank(deleteOp)) {
            for (T entity : entities) {
                aclChecker.check(entity, deleteOp);
            }
        }

        if (serviceImpl.deleteByEntities(entities)) {
            return Response.success(entities);
        }

        return Response.error(-995);
    }

    /**
     * 对即将新增的实体进行调整.
     * @param entity 即将新增的实体
     * @return 调整后的实体
     */
    @Nonnull
    protected T alter(T entity) {
        return entity;
    }

    /**
     * 对即将修改的实体进行调整.
     * @param old 原实体
     * @param entity 新实体
     * @return 调整后的实体
     */
    @Nonnull
    protected T alter(@Nonnull T old, @Nonnull T entity) {
        return entity;
    }

}
