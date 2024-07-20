package com.apzda.cloud.gsvc.utils;

import java.util.regex.Pattern;

public abstract class StringUtils {

    public static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_([a-z])");

    public static final Pattern CAMEL_PATTERN = Pattern.compile("([A-Z])");

    public static final String STRING_NULL = "null";

    public static String toUnderscore(String str) {
        if (org.apache.commons.lang3.StringUtils.isBlank(str)) {
            return str;
        }
        return CAMEL_PATTERN.matcher(lowerFirst(str)).replaceAll(m -> "_" + m.group(1).toLowerCase());
    }

    public static String camelToUnderline(String str) {
        return toUnderscore(str);
    }

    public static String toDashed(String str) {
        if (org.apache.commons.lang3.StringUtils.isBlank(str)) {
            return str;
        }
        return CAMEL_PATTERN.matcher(lowerFirst(str)).replaceAll(m -> "-" + m.group(1).toLowerCase());
    }

    public static String toCamel(String underscoreStr) {
        if (org.apache.commons.lang3.StringUtils.isBlank(underscoreStr)) {
            return underscoreStr;
        }

        return UNDERSCORE_PATTERN.matcher(lowerFirst(underscoreStr)).replaceAll(m -> m.group(1).toUpperCase());
    }

    public static String lowerFirst(String str) {
        if (org.apache.commons.lang3.StringUtils.isBlank(str)) {
            return str;
        }
        return org.apache.commons.lang3.StringUtils.lowerCase(String.valueOf(str.charAt(0))) + str.substring(1);
    }

    public static boolean isEmpty(Object object) {
        if (object == null) {
            return (true);
        }
        if ("".equals(object)) {
            return (true);
        }
        return "null".equals(object);
    }

    public static boolean isNotEmpty(Object object) {
        return object != null && !"".equals(object) && !STRING_NULL.equals(object);
    }

}
