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
package com.apzda.cloud.rocketmq.listener;

import com.apzda.cloud.rocketmq.IMail;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
public interface ISendCallback {

    Logger log = LoggerFactory.getLogger(ISendCallback.class);

    void onSuccess(Object message);

    void onError(Object message, Throwable e);

    static void onSuccess(@Nonnull IMail mail, List<ISendCallback> callbacks, @Nonnull ObjectMapper objectMapper) {
        if (CollectionUtils.isEmpty(callbacks)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                val payload = mail.payload(objectMapper);
                for (ISendCallback callback : callbacks) {
                    try {
                        callback.onSuccess(payload);
                    }
                    catch (Exception e) {
                        log.error("Callback {} failed: {}", callback, e.getMessage(), e);
                    }
                }
            }
            catch (Exception e) {
                log.error("Cannot get the payload from: {}", mail, e);
            }
        });
    }

    static void onError(@Nonnull IMail mail, List<ISendCallback> callbacks, @Nonnull ObjectMapper objectMapper,
            @Nonnull Throwable e) {
        if (CollectionUtils.isEmpty(callbacks)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                val payload = mail.payload(objectMapper);
                for (val callback : callbacks) {
                    try {
                        callback.onError(payload, e);
                    }
                    catch (Exception ee) {
                        log.error("Callback {} failed: {}", callback, ee.getMessage(), ee);
                    }
                }
            }
            catch (Exception ex) {
                log.error("Cannot get the payload from: {}", mail, ex);
            }
        });
    }

}
