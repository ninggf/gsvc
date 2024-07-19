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

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Slf4j
public class RedisInfraImpl implements Counter, TempStorage {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public int count(@NonNull String key, long interval) {
        Assert.isTrue(interval > 0, "interval = " + interval + " <= 0");
        val a = DateUtil.currentSeconds() / interval;
        val id = "counter." + key + "." + a;
        try {
            var increment = stringRedisTemplate.opsForValue().increment(id);
            if (increment == null) {
                increment = Long.parseLong(Objects.requireNonNull(stringRedisTemplate.opsForValue().get(id)));
            }
            return Math.toIntExact(increment);
        }
        catch (Exception e) {
            log.warn("Cannot get try count for {} - {}", id, e.getMessage());
            return Integer.MAX_VALUE;
        }
        finally {
            try {
                stringRedisTemplate.expire(id, interval + 1, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                log.warn("Cannot set expired time of the key of count {} - {}", id, e.getMessage());
            }
        }
    }

    @Override
    public <T extends ExpiredData> T save(@NonNull String id, @NonNull T data) throws Exception {
        val key = "storage." + id;
        val ca = objectMapper.writeValueAsString(data);
        stringRedisTemplate.opsForValue().set(key, ca, data.getExpireTime().toSeconds(), TimeUnit.SECONDS);
        return data;
    }

    @Override
    @NonNull
    public <T extends ExpiredData> Optional<T> load(@NonNull String id, @NonNull Class<T> tClass) {
        val key = "storage." + id;
        try {
            val value = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(value)) {
                return Optional.of(objectMapper.readValue(value, tClass));
            }
        }
        catch (Exception e) {
            log.error("Cannot load  TempData({}): {}", id, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void remove(@NonNull String id) {
        val key = "storage." + id;
        try {
            stringRedisTemplate.delete(key);
        }
        catch (Exception e) {
            log.warn("Cannot delete TempData({}): {}", id, e.getMessage());
        }
    }

}
