/*
 * Copyright (C) 2023-2025 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.mybatis.plugin;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.1
 * @since 3.4.1
 **/
public class OptimisticLockerInnerInterceptor extends com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor {
    public OptimisticLockerInnerInterceptor() {
        this(false);
    }

    public OptimisticLockerInnerInterceptor(boolean wrapperMode) {
        super(wrapperMode);
    }

    private static class VersionFactory {
        private static final Map<Class<?>, Function<Object, Object>> VERSION_FUNCTION_MAP = new HashMap<>();

        static {
            VERSION_FUNCTION_MAP.put(long.class, version -> (long) version + 1);
            VERSION_FUNCTION_MAP.put(Long.class, version -> (long) version + 1);
            VERSION_FUNCTION_MAP.put(short.class, version -> (short) ((short) version + (short) 1));
            VERSION_FUNCTION_MAP.put(Short.class, version -> (short) ((short) version + (short) 1));
            VERSION_FUNCTION_MAP.put(int.class, version -> (int) version + 1);
            VERSION_FUNCTION_MAP.put(Integer.class, version -> (int) version + 1);
            VERSION_FUNCTION_MAP.put(Date.class, version -> new Date());
            VERSION_FUNCTION_MAP.put(Timestamp.class, version -> new Timestamp(System.currentTimeMillis()));
            VERSION_FUNCTION_MAP.put(LocalDateTime.class, version -> LocalDateTime.now());
            VERSION_FUNCTION_MAP.put(Instant.class, version -> Instant.now());
        }

        public static Object getUpdatedVersionVal(Class<?> clazz, Object originalVersionVal) {
            Function<Object, Object> versionFunction = VERSION_FUNCTION_MAP.get(clazz);
            if (versionFunction == null) {
                return originalVersionVal;
            }
            return versionFunction.apply(originalVersionVal);
        }
    }

    protected Object getUpdatedVersionVal(Class<?> clazz, Object originalVersionVal) {
        return VersionFactory.getUpdatedVersionVal(clazz, originalVersionVal);
    }
}
