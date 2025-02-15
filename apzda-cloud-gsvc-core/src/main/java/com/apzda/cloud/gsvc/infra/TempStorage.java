/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.infra;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface TempStorage {

    @Nullable
    <T extends ExpiredData> T save(@NonNull String id, @NonNull T data) throws Exception;

    @NonNull
    <T extends ExpiredData> Optional<T> load(@NonNull String id, @NonNull Class<T> tClass);

    @NonNull
    @SuppressWarnings("unchecked")
    default <T extends ExpiredData> T load(@NonNull String id, @NonNull T defaultValue) {
        return load(id, defaultValue.getClass()).map(expiredData -> (T) expiredData).orElse(defaultValue);
    }

    void remove(@NonNull String id);

    void expire(@NonNull String id, Duration duration);

    @NonNull
    Duration getDuration(@NonNull String id);

    @NonNull
    Lock getLock(@NonNull String id);

    void deleteLock(@NonNull String id);

}
