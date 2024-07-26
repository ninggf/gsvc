package com.apzda.cloud.boot.query;

import com.apzda.cloud.gsvc.utils.StringUtils;
import lombok.Getter;

/**
 * 查询链接规则
 */
@Getter
public enum MatchTypeEnum {

    /** 查询链接规则 AND */
    AND("AND"),
    /** 查询链接规则 OR */
    OR("OR");

    private final String value;

    MatchTypeEnum(String value) {
        this.value = value;
    }

    public static MatchTypeEnum getByValue(Object value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return getByValue(value.toString());
    }

    public static MatchTypeEnum getByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (MatchTypeEnum val : values()) {
            if (val.getValue().equalsIgnoreCase(value)) {
                return val;
            }
        }
        return null;
    }

}
