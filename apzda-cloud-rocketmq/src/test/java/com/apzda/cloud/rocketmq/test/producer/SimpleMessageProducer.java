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
package com.apzda.cloud.rocketmq.test.producer;

import com.apzda.cloud.rocketmq.config.RocketConfigProperties;
import com.apzda.cloud.rocketmq.producer.RocketmqProducer;
import com.apzda.cloud.rocketmq.test.Tags;
import com.apzda.cloud.rocketmq.test.message.SimpleMessage;
import jakarta.annotation.Nonnull;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
public class SimpleMessageProducer extends RocketmqProducer<SimpleMessage, Tags> {

    public SimpleMessageProducer(@Nonnull RocketConfigProperties.ProducerConfig config) {
        super(config);
    }

}
