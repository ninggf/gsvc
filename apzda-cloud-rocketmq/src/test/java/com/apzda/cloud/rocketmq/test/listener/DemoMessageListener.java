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
package com.apzda.cloud.rocketmq.test.listener;

import com.apzda.cloud.rocketmq.listener.IMessageListener;
import com.apzda.cloud.rocketmq.test.Tags;
import com.apzda.cloud.rocketmq.test.message.SimpleMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.jetbrains.annotations.NotNull;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Slf4j
public class DemoMessageListener implements IMessageListener<SimpleMessage, Tags> {

    @Override
    public boolean onMessage(@NotNull SimpleMessage message, Tags tag, @NotNull MessageView messageView) {
        log.info("Receive message from topic: {}, tag: {}, message: {}", messageView.getTopic(), tag,
                message.getContent());
        return true;
    }

}
