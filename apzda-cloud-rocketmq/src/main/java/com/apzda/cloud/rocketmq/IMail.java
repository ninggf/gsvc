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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
public interface IMail {

    String getMailId();

    void setMailId(String id);

    String getContent();

    void setContent(String content);

    String getRecipients();

    void setRecipients(String recipients);

    Boolean getTransactional();

    void setTransactional(Boolean transactional);

    boolean isAsync();

    void setAsync(boolean async);

    String getContentType();

    void setContentType(String contentType);

    Long getPostTime();

    void setPostTime(Long postTime);

    void addProperty(String key, String value);

    Map<String, String> getProperties();

    default Object payload(ObjectMapper objectMapper) throws ClassNotFoundException, JsonProcessingException {
        val contentType = getContentType();
        val content = getContent();
        if (StringUtils.isNotBlank(contentType) && StringUtils.isNotBlank(content)) {
            val cClz = Class.forName(contentType);
            return objectMapper.readValue(content, cClz);
        }
        throw new IllegalStateException(String.format("contentType or content of %s is empty", getMailId()));
    }

}
