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
package com.apzda.cloud.boot.sanitize;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class RegexpSanitizer implements Sanitizer<String> {

    @Override
    @SuppressWarnings("all")
    public String sanitize(String text, String[] configure) {
        if (StringUtils.isBlank(text) && configure.length < 2) {
            return text;
        }

        try {
            int flag = 0;
            if (configure.length == 3) {
                flag = getFlags(configure[2]);
            }
            val regexp = Pattern.compile(configure[0], flag);
            return regexp.matcher(text).replaceAll(configure[1]);
        }
        catch (Exception e) {
            log.warn("Cannot sanitize '{}': {}", text, e.getMessage());
            return text;
        }
    }

    private int getFlags(String s) {
        return Integer.parseInt(s.trim());
    }

}
