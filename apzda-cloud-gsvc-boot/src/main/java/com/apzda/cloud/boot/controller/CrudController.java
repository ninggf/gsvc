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
import com.apzda.cloud.gsvc.dto.Audit;
import com.apzda.cloud.gsvc.dto.PageResult;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.NotFoundError;
import com.apzda.cloud.gsvc.event.AuditEvent;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.model.IEntity;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import com.apzda.cloud.mybatis.utils.PagerUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.Clock;
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

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private Clock clock;

    private String resourceId;

    private String createPermission;

    private String readPermission;

    private String updatePermission;

    private String deletePermission;

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
                createPermission = resource.create() + ":" + resourceId;
            }
            if (StringUtils.isNotBlank(resource.read())) {
                readPermission = resource.read() + ":" + resourceId;
            }
            if (StringUtils.isNotBlank(resource.update())) {
                updatePermission = resource.update() + ":" + resourceId;
            }
            if (StringUtils.isNotBlank(resource.delete())) {
                deletePermission = resource.delete() + ":" + resourceId;
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
    public Response<PageResult<T>> list(T entity, GsvcExt.Pager pager, HttpServletRequest req) {
        if (StringUtils.isNotBlank(readPermission)) {
            aclChecker.check(entity, readPermission);
        }

        IPage<T> page = PagerUtils.iPage(pager);
        QueryWrapper<T> query = alter(QueryGenerator.initQueryWrapper(entity, req.getParameterMap()), entity, req);
        val result = serviceImpl.read(page, query);

        return Response.success(PagerUtils.toResult(result));
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
        if (StringUtils.isNotBlank(readPermission)) {
            aclChecker.check(entity, readPermission);
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
        val audit = new Audit();
        audit.setActivity("Create Entity");
        audit.setTemplate(true);
        try {
            audit.setNewValue(entity);
            if (StringUtils.isNotBlank(createPermission)) {
                aclChecker.check(entity, createPermission);
            }
            val inst = serviceImpl.create(alter(entity));
            if (inst != null) {
                audit.setOldValue(entity);
                audit.setNewValue(inst);
                audit.setMessage("entity created successfully");
                return Response.success(inst);
            }
            audit.setMessage("entity not created: {}");
            audit.setLevel("warn");
            audit.getArgs().add(I18nUtils.t("error.995"));
            return Response.error(-995);
        }
        catch (AccessDeniedException | AuthenticationException ae) {
            audit.setMessage("entity not created: {}");
            audit.setLevel("warn");
            audit.getArgs().add(ae.getMessage());
            throw ae;
        }
        finally {
            publishAuditEvent(audit);
        }
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
        val audit = new Audit();
        audit.setActivity("Update Entity");
        audit.setTemplate(true);
        try {
            audit.setNewValue(entity);
            val o = serviceImpl.read(id);
            if (o == null) {
                throw new GsvcException(new NotFoundError(resourceId, String.valueOf(id)));
            }
            audit.setOldValue(o);
            if (StringUtils.isNotBlank(updatePermission)) {
                aclChecker.check(entity, updatePermission);
            }
            if (serviceImpl.update(id, alter(o, entity))) {
                audit.setMessage("entity updated successfully");
                return Response.success(serviceImpl.read(id));
            }
            audit.setMessage("entity not updated: {}");
            audit.setLevel("warn");
            audit.getArgs().add(I18nUtils.t("error.995"));
            return Response.error(-995);
        }
        catch (AccessDeniedException | AuthenticationException ae) {
            audit.setMessage("entity not updated: {}");
            audit.setLevel("warn");
            audit.getArgs().add(ae.getMessage());
            throw ae;
        }
        finally {
            publishAuditEvent(audit);
        }
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
        val audit = new Audit();
        audit.setActivity("Delete Entity");
        audit.setTemplate(true);
        try {
            val o = serviceImpl.read(id);
            if (o == null) {
                throw new GsvcException(new NotFoundError(resourceId, String.valueOf(id)));
            }
            audit.setOldValue(o);
            if (StringUtils.isNotBlank(deletePermission)) {
                aclChecker.check(o, deletePermission);
            }

            if (serviceImpl.delete(id)) {
                audit.setMessage("entity deleted successfully");
                return Response.success(o);
            }
            audit.setMessage("entity not deleted: {}");
            audit.setLevel("warn");
            audit.getArgs().add(I18nUtils.t("error.995"));
            return Response.error(-995);
        }
        catch (AccessDeniedException | AuthenticationException ae) {
            audit.setMessage("entity not deleted: {}");
            audit.setLevel("warn");
            audit.getArgs().add(ae.getMessage());
            throw ae;
        }
        finally {
            publishAuditEvent(audit);
        }
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

        val audit = new Audit();
        audit.setActivity("Delete Entity");
        audit.setTemplate(true);

        try {
            audit.setOldValue(entities);
            if (StringUtils.isNotBlank(deletePermission)) {
                for (T entity : entities) {
                    aclChecker.check(entity, deletePermission);
                }
            }

            if (serviceImpl.deleteByEntities(entities)) {
                audit.setMessage("entity deleted successfully");
                return Response.success(entities);
            }

            audit.setMessage("entity not deleted: {}");
            audit.setLevel("warn");
            audit.getArgs().add(I18nUtils.t("error.995"));
            return Response.error(-995);
        }
        catch (AccessDeniedException | AuthenticationException ae) {
            audit.setMessage("entity not deleted: {}");
            audit.setLevel("warn");
            audit.getArgs().add(ae.getMessage());
            throw ae;
        }
        finally {
            publishAuditEvent(audit);
        }
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

    @Nonnull
    protected QueryWrapper<T> alter(QueryWrapper<T> query, T entity, HttpServletRequest req) {
        return query;
    }

    private void publishAuditEvent(Audit audit) {
        if (eventPublisher != null) {
            if (clock == null) {
                eventPublisher.publishEvent(new AuditEvent(audit));
            }
            else {
                eventPublisher.publishEvent(new AuditEvent(audit, clock));
            }
        }
    }

}
