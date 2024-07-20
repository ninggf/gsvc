package com.apzda.cloud.boot.query;

import lombok.Getter;

import static com.apzda.cloud.gsvc.utils.StringUtils.isEmpty;

@Getter
public enum QueryRuleEnum {

    /** 查询规则 大于 */
    GT(">", "gt", "大于"),
    /** 查询规则 大于等于 */
    GE(">=", "ge", "大于等于"),
    /** 查询规则 小于 */
    LT("<", "lt", "小于"),
    /** 查询规则 小于等于 */
    LE("<=", "le", "小于等于"),
    /** 查询规则 等于 */
    EQ("=", "eq", "等于"),
    /** 查询规则 不等于 */
    NE("!=", "ne", "不等于"),
    /** 查询规则 包含 */
    IN("IN", "in", "包含"),
    /** 查询规则 全模糊 */
    LIKE("LIKE", "like", "全模糊"),
    /** 查询规则 左模糊 */
    LEFT_LIKE("LEFT_LIKE", "left_like", "左模糊"),
    /** 查询规则 右模糊 */
    RIGHT_LIKE("RIGHT_LIKE", "right_like", "右模糊"),
    /** 查询规则 带加号等于 */
    EQ_WITH_ADD("EQWITHADD", "eq_with_add", "带加号等于"),
    /** 查询规则 多词模糊匹配 */
    LIKE_WITH_AND("LIKEWITHAND", "like_with_and", "多词模糊匹配"),
    /** 查询规则 自定义SQL片段 */
    SQL_RULES("USE_SQL_RULES", "ext", "自定义SQL片段");

    private final String value;

    private final String condition;

    private final String msg;

    QueryRuleEnum(String value, String condition, String msg) {
        this.value = value;
        this.condition = condition;
        this.msg = msg;
    }

    public static QueryRuleEnum getByValue(String value) {
        if (isEmpty(value)) {
            return null;
        }
        for (QueryRuleEnum val : values()) {
            if (val.getValue().equals(value) || val.getCondition().equals(value)) {
                return val;
            }
        }
        return null;
    }

}
