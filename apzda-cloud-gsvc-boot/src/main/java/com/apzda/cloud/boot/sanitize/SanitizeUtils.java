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
package com.apzda.cloud.boot.sanitize;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@SuppressWarnings("rawtypes")
public abstract class SanitizeUtils {

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(@Nonnull ApplicationContext applicationContext) {
        SanitizeUtils.applicationContext = applicationContext;
    }

    private static final LoadingCache<Class<? extends Sanitizer>, Sanitizer> cache = CacheBuilder.newBuilder()
        .build(new CacheLoader<>() {
            @Override
            @Nonnull
            public Sanitizer<?> load(@Nonnull Class<? extends Sanitizer> key) throws Exception {
                val sanitizer = BeanUtils.instantiateClass(key);
                if (applicationContext != null && sanitizer instanceof ApplicationContextAware aware) {
                    aware.setApplicationContext(applicationContext);
                }
                return sanitizer;
            }
        });

    /**
     * 根据消毒器类型获取消毒器实现.
     * @param key 消毒器类型
     * @return 消毒器实例
     */
    @Nullable
    public static Sanitizer getSanitizer(@Nonnull Class<? extends Sanitizer> key) {
        try {
            return cache.get(key);
        }
        catch (Exception e) {
            log.warn("Sanitizer: {} not found!", key);
            return null;
        }
    }

    @Nullable
    public static <T> T sanitize(@Nullable T data) {
        if (data == null) {
            return null;
        }

        if (BeanUtils.isSimpleProperty(data.getClass())) {
            return data;
        }

        val properties = BeanUtil.getPropertyDescriptorMap(data.getClass(), false);
        if (CollectionUtils.isEmpty(properties)) {
            return data;
        }

        val fields = com.apzda.cloud.gsvc.utils.BeanUtils.getAllFieldsMap(data);

        for (Map.Entry<String, PropertyDescriptor> property : properties.entrySet()) {
            val name = property.getKey();
            val pd = property.getValue();
            val method = pd.getReadMethod();
            val writeMethod = pd.getWriteMethod();

            if (method != null && writeMethod != null) {
                try {
                    val field = fields.get(name);
                    writeMethod.invoke(data, sanitize(field, method, method.invoke(data)));
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    log.warn("Cannot get value of property [{}] from [{}]", name, data.getClass());
                }
            }
        }

        return data;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T sanitize(@Nonnull Field field, @Nonnull Method method, @Nullable T value) {
        if (value == null) {
            return null;
        }

        var annotation = field.getAnnotation(Sanitized.class);

        if (annotation == null) {
            annotation = method.getAnnotation(Sanitized.class);
        }

        if (annotation != null) {
            val sanitizer = SanitizeUtils.getSanitizer(annotation.sanitizer());
            if (sanitizer != null && sanitizer.supports(value)) {
                val configure = annotation.value();
                return (T) sanitizer.sanitize(value, configure);
            }
        }

        return value;
    }

}
