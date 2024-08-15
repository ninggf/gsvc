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
package com.apzda.cloud.boot.dict;

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

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@SuppressWarnings("rawtypes")
public abstract class TransformUtils {

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(@Nonnull ApplicationContext applicationContext) {
        TransformUtils.applicationContext = applicationContext;
    }

    private static final LoadingCache<Class<? extends Transformer>, Transformer> cache = CacheBuilder.newBuilder()
        .build(new CacheLoader<>() {
            @Override
            @Nonnull
            public Transformer load(@Nonnull Class<? extends Transformer> key) throws Exception {
                val transformer = BeanUtils.instantiateClass(key);
                if (applicationContext != null && transformer instanceof ApplicationContextAware aware) {
                    aware.setApplicationContext(applicationContext);
                }
                return transformer;
            }
        });

    /**
     * 根据转换器类型获取转换器实例.
     * @param key 转换器类型
     * @return 转换器实例
     */
    @Nullable
    public static Transformer getTransformer(@Nonnull Class<? extends Transformer> key) {
        try {
            return cache.get(key);
        }
        catch (Exception e) {
            log.warn("Transformer: {} not found!", key);
            return null;
        }
    }

}
