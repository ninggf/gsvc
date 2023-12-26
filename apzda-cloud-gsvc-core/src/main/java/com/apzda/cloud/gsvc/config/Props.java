/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class Props {

    private static final Logger log = LoggerFactory.getLogger(Props.class);

    private final Map<String, String> props;

    public Props(Map<String, String> props) {
        this.props = props;
    }

    public String get(String name) {
        return props.get(name);
    }

    public String getString(String name, String defValue) {
        return props.getOrDefault(name, defValue);
    }

    public int getInt(String name, int defValue) {
        String value = props.get(name);
        if (value == null || value.isBlank()) {
            return defValue;
        }
        try {
            return Integer.parseInt(value);
        }
        catch (Exception e) {
            log.warn("the {}({}) is not valid, use {} as default", name, value, defValue);
            return defValue;
        }
    }

    public long getLong(String name, long defValue) {
        String value = props.get(name);
        if (value == null || value.isBlank()) {
            return defValue;
        }
        try {
            return Long.parseLong(value);
        }
        catch (Exception e) {
            log.warn("the {}({}) is not valid, use {} as default", name, value, defValue);
            return defValue;
        }
    }

    public double getDouble(String name, double defValue) {
        String value = props.get(name);
        if (value == null || value.isBlank()) {
            return defValue;
        }
        try {
            return Double.parseDouble(value);
        }
        catch (Exception e) {
            log.warn("the {}({}) is not valid, use {} as default", name, value, defValue);
            return defValue;
        }
    }

    public float getFloat(String name, float defValue) {
        String value = props.get(name);
        if (value == null || value.isBlank()) {
            return defValue;
        }
        try {
            return Float.parseFloat(value);
        }
        catch (Exception e) {
            log.warn("the {}({}) is not valid, use {} as default", name, value, defValue);
            return defValue;
        }
    }

    public boolean getBool(String name, boolean defValue) {
        String value = props.get(name);
        if (value == null || value.isBlank()) {
            return defValue;
        }
        return Boolean.parseBoolean(value);
    }

}
