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
package com.apzda.cloud.rocketmq.domain.entity;

import com.apzda.cloud.rocketmq.IMail;
import com.apzda.cloud.rocketmq.domain.vo.MailStatus;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Data
@TableName("apzda_mq_mailbox")
public class Mailbox implements IMail {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    private Long nextRetryAt;

    private Boolean transactional;

    private String mailId;

    private MailStatus status;

    private String recipients;

    private Long postTime;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> properties;

    private String msgId;

    protected String contentType;

    private Integer retries;

    private String remark;

    @TableField(exist = false)
    private boolean async;

    // @Override
    public void addProperty(String key, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }

}
