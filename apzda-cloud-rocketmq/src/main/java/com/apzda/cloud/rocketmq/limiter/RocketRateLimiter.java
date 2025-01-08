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
package com.apzda.cloud.rocketmq.limiter;

import com.apzda.cloud.gsvc.infra.Counter;
import com.apzda.cloud.rocketmq.IMail;
import com.apzda.cloud.rocketmq.config.RocketConfigProperties;
import com.apzda.cloud.rocketmq.listener.ISendCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.java.exception.TooManyRequestsException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Slf4j
public class RocketRateLimiter implements RateLimiter {

    private final static String COUNTER_KEY = "RLK";

    private final Counter counter;

    private final ObjectMapper objectMapper;

    private final int limit;

    private final long interval;

    private final int maxAttempts;

    private final List<ISendCallback> callbacks;

    private final ExecutorService executor;

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public RocketRateLimiter(@Nonnull Counter counter, @Nonnull ObjectMapper objectMapper,
            @Nonnull RocketConfigProperties properties, List<ISendCallback> callbacks) {
        val limitCfg = properties.getLimit();
        this.limit = limitCfg.getLimit();
        this.interval = limitCfg.getInterval().toSeconds();
        this.maxAttempts = limitCfg.getMaxAttempts();
        this.counter = counter;
        this.objectMapper = objectMapper;
        val cpu = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(cpu, r -> {
            val thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("RateLimiter-" + atomicInteger.getAndAdd(1));
            return thread;
        });
        this.callbacks = callbacks;
    }

    @PreDestroy
    public void close() {
        if (executor == null) {
            return;
        }
        try {
            executor.shutdown();
            if (executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.info("Shutdown RateLimiter executor successfully!");
            }
            else {
                log.warn("Shutdown RateLimiter executor timeout: 30s");
            }
        }
        catch (Exception e) {
            log.warn("Cannot shutdown RateLimiter executor: {}", e.getMessage());
        }
    }

    @Override
    public void send(@Nonnull Producer producer, @Nonnull Message message, IMail mail) throws Exception {
        if (mail.isAsync()) {
            executor.submit(() -> {
                try {
                    _send(producer, message, mail, 0);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            _send(producer, message, mail, 0);
        }
    }

    private void _send(@Nonnull Producer producer, @Nonnull Message message, IMail mail, int attempt) throws Exception {
        int wait = 0;
        int valve = counter.count(COUNTER_KEY, interval);
        while (valve >= limit && wait < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            }
            catch (InterruptedException ignored) {
            }
            wait++;
            valve = counter.count(COUNTER_KEY, interval);
        }

        try {
            if (valve >= limit) {
                log.warn("Max requests reached: {}/{}s", limit, interval);
                throw new TooManyRequestsException(429, mail.getMailId(),
                        String.format("Max requests reached: %d/%ds", limit, interval));
            }
            producer.send(message);
            if (mail.isAsync()) {
                ISendCallback.onSuccess(mail, callbacks, objectMapper);
            }
        }
        catch (TooManyRequestsException e) {
            if (attempt < maxAttempts) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                }
                catch (InterruptedException ignored) {
                }
                _send(producer, message, mail, attempt + 1);
            }
            else if (mail.isAsync()) {
                ISendCallback.onError(mail, callbacks, objectMapper, e);
            }
            else {
                throw e;
            }
        }
        catch (ClientException e) {
            if (mail.isAsync()) {
                ISendCallback.onError(mail, callbacks, objectMapper, e);
            }
            else {
                throw e;
            }
        }
    }

}
