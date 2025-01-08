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
package com.apzda.cloud.rocketmq.test.callback;

import com.apzda.cloud.rocketmq.listener.ISendCallback;
import lombok.extern.slf4j.Slf4j;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Slf4j
public class DemoSendCallback implements ISendCallback {

    @Override
    public void onSuccess(Object message) {
        log.info("信件发送成功啦: {}", message);
    }

    @Override
    public void onError(Object message, Throwable e) {
        log.info("信件发送失败啦: {}, MessageId: {}", message, e.getMessage());
    }

}
