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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class LocalInfraImpl implements Counter, TempStorage {

    private final Cache<String, Object> storageCache;

    private final LoadingCache<String, AtomicInteger> counterCache;

    @Getter
    private final TreeMap<Long, Set<String>> keys = new TreeMap<>();

    private final ScheduledExecutorService cleaner;

    public LocalInfraImpl(Duration tempMaxExpiredTime) {
        storageCache = CacheBuilder.newBuilder().expireAfterAccess(tempMaxExpiredTime).build();
        counterCache = CacheBuilder.newBuilder().expireAfterAccess(tempMaxExpiredTime).build(new CacheLoader<>() {
            @Override
            @NonNull
            public AtomicInteger load(@NonNull String key) throws Exception {
                return new AtomicInteger(0);
            }
        });

        cleaner = Executors.newScheduledThreadPool(1);

        cleaner.scheduleWithFixedDelay(() -> {
            val current = DateUtil.currentSeconds();
            var key = keys.firstKey();
            while (key != null && key < current) {
                val ids = keys.remove(key);
                counterCache.invalidateAll(ids);
                storageCache.invalidateAll(ids);
                key = keys.firstKey();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public int count(@NonNull String key, long interval) {
        Assert.isTrue(interval > 0, "interval = " + interval + " <= 0");
        val a = DateUtil.currentSeconds() / interval;
        val id = "counter." + key + a;
        try {
            val ai = counterCache.get(id);
            setExpired(id, Duration.ofSeconds(interval + 1));
            return ai.addAndGet(1);
        }
        catch (Exception e) {
            log.warn("Cannot count (key={}, id={}) - {}", key, id, e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public <T extends ExpiredData> T save(@NonNull String id, @NonNull T data) throws Exception {
        val key = "storage." + id;
        storageCache.put(key, data);
        setExpired(key, data.getExpireTime());
        return data;
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends ExpiredData> Optional<T> load(@NonNull String id, @NonNull Class<T> tClass) {
        val key = "storage." + id;
        val data = storageCache.getIfPresent(key);
        if (data != null && data.getClass().isAssignableFrom(tClass)) {
            return Optional.of((T) data);
        }
        return Optional.empty();
    }

    @Override
    public void remove(@NonNull String id) {
        val key = "storage." + id;
        storageCache.invalidate(key);
    }

    public long getCounterSize() {
        return counterCache.size();
    }

    public void shutdown() {
        cleaner.shutdown();
    }

    private void setExpired(String id, Duration expired) {
        val expiredTime = DateUtil.currentSeconds() + expired.toSeconds();
        synchronized (keys) {
            keys.compute(expiredTime, (key, v) -> {
                if (v == null) {
                    v = new HashSet<>();
                }
                v.add(id);
                return v;
            });
        }
    }

}
