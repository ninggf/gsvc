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
package com.apzda.cloud.rocketmq;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Mail implements IMail {

    protected String mailId;

    protected String recipients;

    protected String content;

    protected Long postTime;

    protected Boolean transactional;

    protected boolean async = true;

    protected String contentType;

    private Map<String, String> properties = new HashMap<>();

    @Override
    public String toString() {
        return StrUtil.format("id={}, recipients={}, type={}, content={}", getMailId(), getRecipients(),
                getContentType(), StrUtil.truncateUtf8(getContent(), 256));
    }

    public String getMailId() {
        if (StringUtils.isBlank(mailId)) {
            mailId = UUID.randomUUID().toString();
        }
        return mailId;
    }

    @Override
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

}
