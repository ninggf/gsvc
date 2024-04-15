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
package com.apzda.cloud.gsvc.autoconfigure;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class ConfigureHelper {

    private static String REAL_IP_HEADER = "X-Real-IP";

    private static List<String> REAL_IP_FROM = Collections.emptyList();

    static void setRealIpHeader(String header) {
        REAL_IP_HEADER = header;
    }

    static void setRealIpFrom(String ipAddresses) {
        if (StringUtils.isNotBlank(ipAddresses)) {
            REAL_IP_FROM = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(ipAddresses);
        }
    }

    public static String getRealIpHeader() {
        return REAL_IP_HEADER;
    }

    public static List<String> getRealIpFrom() {
        return REAL_IP_FROM;
    }

}
