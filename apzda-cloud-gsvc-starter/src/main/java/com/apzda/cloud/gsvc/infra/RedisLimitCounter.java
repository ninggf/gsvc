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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Slf4j
public class RedisLimitCounter implements Counter, TempStorage {

    @Override
    public int count(@NonNull String key, long interval) {
        return 0;
    }

    @Override
    public <T extends TempData> T save(@NonNull String id, @NonNull T data) throws Exception {
        return null;
    }

    @Override
    public <T extends TempData> T load(@NonNull String id) throws Exception {
        return null;
    }

    @Override
    public void remove(@NonNull String id) {

    }

}
