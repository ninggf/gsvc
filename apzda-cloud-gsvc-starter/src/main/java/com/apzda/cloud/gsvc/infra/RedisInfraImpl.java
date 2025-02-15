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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Slf4j
public class RedisInfraImpl implements Counter, TempStorage {

    private final static Pattern ID_PATTERN = Pattern.compile("^(.+?)@(.+)$");

    private static final Map<String, Lock> locks = new ConcurrentHashMap<>();

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final LoadingCache<String, Boolean> idCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(10))
        .build(new CacheLoader<>() {
            @Override
            @NonNull
            public Boolean load(@NonNull String key) {
                try {
                    val matcher = ID_PATTERN.matcher(key);
                    if (matcher.matches()) {
                        val id = matcher.group(1);
                        val interval = Long.parseLong(matcher.group(2));
                        stringRedisTemplate.expire(id, interval + 1, TimeUnit.SECONDS);
                    }
                }
                catch (Exception e) {
                    log.warn("Cannot set expired time of the key of count {} - {}", key, e.getMessage());
                }
                return true;
            }
        });

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
                idCache.getUnchecked(id + "@" + interval);
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
        val expired = data.getExpireTime();
        if (expired.isZero() || expired.isNegative()) {
            stringRedisTemplate.opsForValue().set(key, ca);
        }
        else {
            stringRedisTemplate.opsForValue().set(key, ca, expired.toSeconds(), TimeUnit.SECONDS);
        }
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

    @Override
    public void expire(@NonNull String id, @NonNull Duration duration) {
        val key = "storage." + id;
        try {
            stringRedisTemplate.expire(key, duration);
        }
        catch (Exception e) {
            log.warn("Cannot expire TempData({}): {}", id, e.getMessage());
        }
    }

    @Override
    @NonNull
    public Duration getDuration(@NonNull String id) {
        val key = "storage." + id;
        try {
            val expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            return Duration.ofSeconds(expire);
        }
        catch (Exception e) {
            log.warn("Cannot get the Expire TempData({}): {}", id, e.getMessage());
        }
        return Duration.ZERO;
    }

    @Override
    @NonNull
    public Lock getLock(@NonNull String id) {
        val key = "lock." + id;

        return locks.computeIfAbsent(key, k -> new SimpleRedisLock(k, stringRedisTemplate));
    }

    @Override
    public void deleteLock(@NonNull String id) {
        val key = "lock." + id;
        locks.remove(key);
    }

    @SuppressWarnings("all")
    static class SimpleRedisLock extends ReentrantLock {

        private final static Duration DEFAULT_TIMEOUT = Duration.ofSeconds(300);

        private final StringRedisTemplate stringRedisTemplate;

        private final String lockName;

        private final ThreadLocal<String> holder = new ThreadLocal<>();

        SimpleRedisLock(String lockName, StringRedisTemplate redisTemplate) {
            super();
            this.lockName = lockName;
            this.stringRedisTemplate = redisTemplate;
        }

        @Override
        public boolean isLocked() {
            return Objects.equals(Thread.currentThread().getName(), holder.get()) && super.isLocked();
        }

        @Override
        public void unlock() {
            try {
                if (isLocked() && isHeldByCurrentThread()) {
                    super.unlock();
                    try {
                        stringRedisTemplate.delete(lockName);
                    }
                    catch (Exception e) {
                        log.error("Cannot unlock {}. Please unlock it manually: DEL {} - {}", lockName, lockName,
                                e.getMessage());
                    }
                }
            }
            catch (Exception ignored) {
                log.error("Cannot unlock {}", lockName);
            }
            finally {
                this.holder.remove();
            }
        }

        @Override
        public void lock() {
            if (!tryLock()) {
                throw new IllegalMonitorStateException(String.format("Cannot lock %s - %s", lockName));
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            if (!tryLock()) {
                throw new IllegalMonitorStateException(String.format("Cannot lock %s", lockName));
            }
        }

        @Override
        public boolean tryLock() {
            try {
                return tryLock(DEFAULT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            }
            catch (RuntimeException re) {
                throw re;
            }
            catch (Exception e) {
                throw new IllegalMonitorStateException(String.format("Cannot lock %s - %s", lockName, e.getMessage()));
            }
        }

        @Override
        public boolean tryLock(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
            val start = System.currentTimeMillis();
            if (Objects.equals(Thread.currentThread().getName(), holder.get()) && isHeldByCurrentThread()) {
                return true;
            }
            if (super.tryLock(timeout, unit)) {
                val end = System.currentTimeMillis();
                lock_(Duration.ofMillis(unit.toMillis(timeout) - (end - start)));
                return true;
            }

            return false;
        }

        private void lock_(Duration duration) throws InterruptedException {
            try {
                long count = Optional.ofNullable(duration).orElse(DEFAULT_TIMEOUT).toMillis() / 100;
                do {
                    val increment = stringRedisTemplate.opsForValue().increment(lockName);
                    if (increment != null && increment == 1) {
                        holder.set(Thread.currentThread().getName());
                        break;
                    }
                    count--;
                    if (count < 0) {
                        throw new InterruptedException(String.format("Timeout while waiting for lock %s", lockName));
                    }
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                while (true);
            }
            catch (InterruptedException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IllegalMonitorStateException(String.format("Cannot lock %s - %s", lockName, e.getMessage()));
            }
        }

    }

}
