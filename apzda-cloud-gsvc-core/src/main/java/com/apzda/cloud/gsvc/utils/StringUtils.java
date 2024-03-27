package com.apzda.cloud.gsvc.utils;

import java.util.regex.Pattern;

public class StringUtils {

    private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_([a-z])");

    private static final Pattern CAMEL_PATTERN = Pattern.compile("([A-Z])");

    public static String toUnderscore(String str) {
        if (org.apache.commons.lang3.StringUtils.isBlank(str)) {
            return str;
        }
        return CAMEL_PATTERN.matcher(lowerFirst(str)).replaceAll(m -> "_" + m.group(1).toLowerCase());
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

}
