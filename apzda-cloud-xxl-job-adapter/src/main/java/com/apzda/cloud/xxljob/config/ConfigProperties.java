/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.xxljob.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@ConfigurationProperties(prefix = "xxl.job")
@Data
public class ConfigProperties {

    private AdminConfigProperties admin = new AdminConfigProperties();

    private ExecutorConfigProperties executor = new ExecutorConfigProperties();

    @Data
    public static class AdminConfigProperties {

        private String accessToken;

        private String addresses;

    }

    @Data
    public static class ExecutorConfigProperties {

        private String appname;

        private String address;

        private String ip;

        private int timeout;

        private int port = 9000;

        private String logPath;

        private int logRetentionDays = 30;

        public String getLogPath() {
            if (StringUtils.hasText(logPath)) {
                return logPath;
            }

            return "/var/logs/" + (StringUtils.hasText(appname) ? appname : "gsvc-job");
        }

    }

}
