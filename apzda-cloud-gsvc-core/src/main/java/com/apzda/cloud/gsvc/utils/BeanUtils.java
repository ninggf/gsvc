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
package com.apzda.cloud.gsvc.utils;

import jakarta.annotation.Nonnull;
import lombok.val;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class BeanUtils {

    @Nonnull
    public static Map<String, Field> getAllFieldsMap(Class<?> clazz) {
        Map<String, Field> fields = new HashMap<>();
        while (clazz != null) {
            for (val f : clazz.getDeclaredFields()) {
                fields.put(f.getName(), f);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    @Nonnull
    public static Map<String, Field> getAllFieldsMap(@Nonnull Object object) {
        Class<?> clazz = object.getClass();
        return getAllFieldsMap(clazz);
    }

    @Nonnull
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        Field[] fields;
        do {
            fields = clazz.getDeclaredFields();
            Collections.addAll(list, fields);
            clazz = clazz.getSuperclass();
        }
        while (clazz != Object.class && clazz != null);

        return list;
    }

    @Nonnull
    public static List<Field> getAllFields(@Nonnull Object object) {
        Class<?> clazz = object.getClass();
        return getAllFields(clazz);
    }

}
