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
package com.apzda.cloud.boot.aop;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.EnumUtil;
import com.apzda.cloud.boot.dict.Dict;
import com.apzda.cloud.boot.dict.DictItem;
import com.apzda.cloud.boot.dict.DictText;
import com.apzda.cloud.boot.mapper.DictItemMapper;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.dto.Response;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apzda.cloud.boot.sanitize.SanitizeUtils.sanitize;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Component
@Aspect
@Slf4j
@Order
@RequiredArgsConstructor
public class DictionaryAdvisor {

    private final ServiceConfigProperties properties;

    private final DictItemMapper dictItemMapper;

    private final static ThreadLocal<Map<String, Map<String, String>>> caches = new ThreadLocal<>();

    @Pointcut("execution(@com.apzda.cloud.boot.dict.Dictionary public com.apzda.cloud.gsvc.dto.Response *(..))")
    public void dictionaryPointcut() {
    }

    @Around("dictionaryPointcut()")
    Object dictionaryAdvice(@Nonnull ProceedingJoinPoint pjp) throws Throwable {
        val returnObj = pjp.proceed();

        if (returnObj instanceof Response<?> response) {
            try {
                val cache = new HashMap<String, Map<String, String>>();
                caches.set(cache);

                val data = response.getData();
                val realReturn = new Response<Object>();
                realReturn.setErrCode(response.getErrCode());
                realReturn.setErrMsg(response.getErrMsg());
                realReturn.setType(response.getType());
                realReturn.setErrType(response.getErrType());
                realReturn.setHttpCode(response.getHttpCode());

                if (data instanceof Collection<?> collection) {
                    realReturn.setData(collection.stream().map(this::fillDictionary).toList());
                }
                else if (data instanceof IPage<?> page) {
                    val newPage = Page.of(page.getCurrent(), page.getSize(), page.getTotal(), page.searchCount());
                    newPage.setRecords(page.getRecords().stream().map(this::fillDictionary).toList());
                    realReturn.setData(newPage);
                }
                else if (data != null && !BeanUtils.isSimpleProperty(data.getClass())) {
                    realReturn.setData(fillDictionary(data));
                }
                if (realReturn.getData() != null) {
                    return realReturn;
                }
            }
            finally {
                caches.remove();
            }
        }

        return returnObj;
    }

    public Object fillDictionary(@Nullable Object data) {
        if (data == null) {
            return null;
        }

        if (BeanUtils.isSimpleProperty(data.getClass())) {
            return data;
        }
        val map = new HashMap<String, Object>();

        val properties = BeanUtil.getPropertyDescriptorMap(data.getClass(), false);
        if (CollectionUtils.isEmpty(properties)) {
            return data;
        }

        val fields = com.apzda.cloud.gsvc.utils.BeanUtils.getAllFieldsMap(data);

        for (Map.Entry<String, PropertyDescriptor> property : properties.entrySet()) {
            val name = property.getKey();
            val pd = property.getValue();
            val method = pd.getReadMethod();
            if (method != null) {
                try {
                    val field = fields.get(name);
                    val value = sanitize(field, method, method.invoke(data));
                    if (value == null) {
                        continue;
                    }
                    map.put(name, value);
                    fillDict(field, name, value, map);
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    log.warn("Cannot get value of property [{}] from [{}]", name, data.getClass());
                }
            }
        }

        return map;
    }

    private void fillDict(@Nonnull Field field, String name, Object value, @Nonnull HashMap<String, Object> map) {
        val annotation = field.getAnnotation(Dict.class);
        if (annotation == null) {
            return;
        }
        var table = annotation.table();
        if (StringUtils.isBlank(table)) {
            val entity = annotation.entity();
            if (entity.isAnnotationPresent(TableName.class)) {
                val ann = entity.getAnnotation(TableName.class);
                table = ann.value();
            }
        }
        val realTable = table;
        val code = StringUtils.defaultIfBlank(annotation.code(), "id");
        val label = annotation.value();
        val dictFieldName = name + StringUtils.defaultIfBlank(this.properties.getConfig().getDictLabelSuffix(), "Text");
        if (EnumUtil.isEnum(value)) {
            map.put(dictFieldName, getTextFromEnum((Enum<?>) value, label));
        }
        else if (StringUtils.isNotBlank(table)) {
            val dict = caches.get().computeIfAbsent(table + "." + code, (key) -> new HashMap<>());
            val dictText = dict.computeIfAbsent(value.toString(),
                    (key) -> getTextFromTable(realTable, code, key, label));
            map.put(dictFieldName, dictText);
        }
        else if (StringUtils.isNotBlank(code)) {
            val dict = caches.get().computeIfAbsent("." + code, (key) -> {
                val kv = new HashMap<String, String>();
                val items = getTextFromDictItemTable(code);
                for (DictItem item : items) {
                    kv.put(item.getVal(), item.getLabel());
                }
                return kv;
            });
            map.put(dictFieldName, dict.get(value.toString()));
        }
        else if (StringUtils.isNotBlank(label)) {
            map.put(dictFieldName, label);
        }
    }

    private List<DictItem> getTextFromDictItemTable(String code) {
        val config = properties.getConfig();
        val dictDelColumn = config.getDictDeletedColumn();
        val dictNotDeletedValue = config.getDictNotDeletedValue();
        val dictLabelColumn = config.getDictLabelColumn();
        if (StringUtils.isNotBlank(dictDelColumn)) {
            return dictItemMapper.getDictLabel(config.getDictItemTable(), config.getDictCodeColumn(), code,
                    config.getDictValueColumn(), dictDelColumn, dictNotDeletedValue, dictLabelColumn);
        }
        else {
            return dictItemMapper.getDictLabel(config.getDictItemTable(), config.getDictCodeColumn(), code,
                    config.getDictValueColumn(), dictLabelColumn);
        }
    }

    private String getTextFromTable(String table, String code, Object value, String label) {
        return dictItemMapper.getDictLabel(table, code, label, value.toString());
    }

    private static Object getTextFromEnum(Enum<?> value, String label) {
        val type = value.getClass();
        val fields = com.apzda.cloud.gsvc.utils.BeanUtils.getAllFieldsMap(value);
        for (Map.Entry<String, Field> kv : fields.entrySet()) {
            val name = kv.getKey();
            val field = kv.getValue();
            if (name.equals(label) || field.getAnnotation(DictText.class) != null) {
                val pd = BeanUtil.getPropertyDescriptor(type, name);
                val method = pd.getReadMethod();
                if (method != null) {
                    try {
                        return method.invoke(value);
                    }
                    catch (IllegalAccessException | InvocationTargetException e) {
                        log.warn("Cannot get value of property [{}] from enum [{}]", name, value.getClass());
                    }
                }
            }
        }

        return EnumUtil.toString(value);
    }

}
