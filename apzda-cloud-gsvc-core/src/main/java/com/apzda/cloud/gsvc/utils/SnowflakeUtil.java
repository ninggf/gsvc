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
package com.apzda.cloud.gsvc.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.SystemPropsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public abstract class SnowflakeUtil {

    private static final String localhostStr = NetUtil.getLocalhostStr();

    private static final String ipv4 = StringUtils.defaultIfBlank(localhostStr, NetUtil.LOCAL_IP);

    private static final long workerId = NetUtil.ipv4ToLong(ipv4) % 32;

    private static final long dc = SystemPropsUtil.getInt("snowflake.dc.id", 1) % 32;

    public static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(workerId, dc);

    static {
        log.info("Snowflake initialized, ipv4={}, Worker ID={}, DataCenter ID={}", ipv4, workerId, dc);
    }

}
