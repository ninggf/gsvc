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
import com.apzda.cloud.boot.dict.DictText;
import com.apzda.cloud.boot.mapper.DictItemMapper;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.dto.Response;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.Map;

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

    @Pointcut("execution(@com.apzda.cloud.boot.dict.Dictionary public com.apzda.cloud.gsvc.dto.Response *(..))")
    public void dictionaryPointcut() {
    }

    @Around("dictionaryPointcut()")
    public Object dictionaryAdvice(ProceedingJoinPoint pjp) throws Throwable {
        val returnObj = pjp.proceed();

        if (returnObj instanceof Response<?> response) {
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

        return returnObj;
    }

    private Object fillDictionary(Object data) {
        if (BeanUtils.isSimpleProperty(data.getClass())) {
            return data;
        }
        val map = new HashMap<String, Object>();

        val properties = BeanUtil.getPropertyDescriptorMap(data.getClass(), true);
        if (CollectionUtils.isEmpty(properties)) {
            return data;
        }

        for (Map.Entry<String, PropertyDescriptor> property : properties.entrySet()) {
            val name = property.getKey();
            val pd = property.getValue();
            val method = pd.getReadMethod();
            if (method != null) {
                try {
                    val fields = getAllFields(data);
                    val value = method.invoke(data);
                    if (value == null) {
                        continue;
                    }
                    map.put(name, value);
                    val field = fields.get(name);
                    val annotation = field.getAnnotation(Dict.class);
                    if (annotation != null) {
                        val table = annotation.table();
                        val code = annotation.code();
                        val label = annotation.value();

                        if (EnumUtil.isEnum(value)) {
                            map.put(name + "Label", getTextFromEnum((Enum<?>) value, label));
                        }
                        else if (StringUtils.isNotBlank(table)) {
                            map.put(name + "Label", getTextFromTable(table, code, value, label));
                        }
                        else if (StringUtils.isNotBlank(code)) {
                            map.put(name + "Label", getTextFromDictItemTable(code, value, label));
                        }
                        else if (StringUtils.isNotBlank(label)) {
                            map.put(name + "Label", label);
                        }
                    }
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    log.warn("Cannot get value of property [{}] from [{}]", name, data.getClass());
                }
            }
        }

        return map;
    }

    private String getTextFromDictItemTable(String code, Object value, String label) {
        val config = properties.getConfig();
        val dictDelColumn = config.getDictDelColumn();
        val dictDelValue = config.getDictDelValue();
        if (StringUtils.isNotBlank(dictDelColumn)) {
            return dictItemMapper.getDictLabel(config.getDictItemTable(), config.getDictCodeColumn(), code,
                    config.getDictValueColumn(), value.toString(), dictDelColumn, dictDelValue, label);
        }
        else {
            return dictItemMapper.getDictLabel(config.getDictItemTable(), config.getDictCodeColumn(), code,
                    config.getDictValueColumn(), value.toString(), label);
        }
    }

    private String getTextFromTable(String table, String code, Object value, String label) {
        return dictItemMapper.getDictLabel(table, code, label, value.toString());
    }

    private static Object getTextFromEnum(Enum<?> value, String label) {
        val type = value.getClass();
        val fields = getAllFields(value);
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

    private static Map<String, Field> getAllFields(Object object) {
        Class<?> clazz = object.getClass();
        Map<String, Field> fields = new HashMap<>();
        while (clazz != null) {
            for (val f : clazz.getDeclaredFields()) {
                fields.put(f.getName(), f);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

}
