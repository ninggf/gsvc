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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class LocalInfraImpl implements Counter, TempStorage {

    private static final Map<String, Lock> locks = new ConcurrentHashMap<>();

    private final Cache<String, Object> storageCache;

    private final LoadingCache<String, AtomicInteger> counterCache;

    @Getter
    private final TreeMap<Long, Set<String>> keys = new TreeMap<>();

    private final Map<String, Long> pointers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner;

    public LocalInfraImpl(Duration tempMaxExpiredTime) {
        storageCache = CacheBuilder.newBuilder().expireAfterAccess(tempMaxExpiredTime).build();
        counterCache = CacheBuilder.newBuilder().expireAfterAccess(tempMaxExpiredTime).build(new CacheLoader<>() {
            @Override
            @NonNull
            public AtomicInteger load(@NonNull String key) {
                return new AtomicInteger(0);
            }
        });

        cleaner = Executors.newScheduledThreadPool(1);

        cleaner.scheduleWithFixedDelay(() -> {
            try {
                val current = DateUtil.currentSeconds();
                var key = keys.firstKey();
                while (key != null && key < current) {
                    val ids = keys.remove(key);
                    counterCache.invalidateAll(ids);
                    storageCache.invalidateAll(ids);
                    ids.forEach(pointers::remove);
                    log.trace("Removed: {}", ids);
                    key = keys.firstKey();
                }
            }
            catch (Exception ignored) {
            }
        }, 1, 1, TimeUnit.SECONDS);
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
    public boolean exist(@NonNull String id) {
        val key = "storage." + id;
        return pointers.containsKey(key);
    }

    @Override
    public void remove(@NonNull String id) {
        val key = "storage." + id;
        storageCache.invalidate(key);
    }

    @Override
    public void expire(@NonNull String id, Duration duration) {
        val key = "storage." + id;
        setExpired(key, duration);
    }

    @Override
    @NonNull
    public Duration getDuration(@NonNull String id) {
        val key = "storage." + id;
        val ep = pointers.get(key);
        if (ep == null) {
            return Duration.ZERO;
        }
        val expire = ep - DateUtil.currentSeconds();
        if (expire <= 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(expire);
    }

    @Override
    @NonNull
    public Lock getLock(@NonNull String id) {
        val key = "lock." + id;
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    @Override
    public void deleteLock(@NonNull String id) {
        val key = "lock." + id;
        locks.remove(key);
    }

    public long getCounterSize() {
        return counterCache.size();
    }

    public void shutdown() {
        cleaner.shutdown();
    }

    private void setExpired(String id, @NonNull Duration expired) {
        val expiredTime = DateUtil.currentSeconds() + expired.toSeconds();
        synchronized (keys) {
            val ep = pointers.get(id);
            if (ep != null) {
                keys.computeIfPresent(ep, (k, v) -> {
                    v.remove(id);
                    return v;
                });
            }
            pointers.put(id, expiredTime);
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
