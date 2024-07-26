package com.apzda.cloud.boot.query;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.apzda.cloud.boot.exception.ColumnNotFoundException;
import com.apzda.cloud.boot.utils.DataSourceUtils;
import com.apzda.cloud.gsvc.utils.BeanUtils;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.apzda.cloud.gsvc.utils.StringUtils;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.StopWatch;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.apzda.cloud.gsvc.utils.StringUtils.isEmpty;
import static com.apzda.cloud.gsvc.utils.StringUtils.isNotEmpty;

/**
 * 查询生成器
 */
@Slf4j
public class QueryGenerator {

    private static final ObjectMapper JSON = ResponseUtils.OBJECT_MAPPER;

    private static final String BEGIN = "_begin";

    private static final String END = "_end";

    /**
     * 数字类型字段，拼接此后缀 接受多值参数
     */
    private static final String MULTI = "_MultiString";

    private static final String STAR = "*";

    private static final String COMMA = ",";

    /**
     * 查询 逗号转义符 相当于一个逗号【作废】
     */
    public static final String QUERY_COMMA_ESCAPE = "++";

    private static final String NOT_EQUAL = "!";

    /** 页面带有规则值查询，空格作为分隔符 */
    private static final String QUERY_SEPARATE_KEYWORD = " ";

    /** 高级查询前端传来的参数名 */
    private static final String SUPER_QUERY_PARAMS = "superQueryParams";

    /** 高级查询前端传来的拼接方式参数名 */
    private static final String SUPER_QUERY_MATCH_TYPE = "superQueryMatchType";

    /** 单引号 */
    public static final String SQL_SQ = "'";

    /** 排序列 */
    private static final String ORDER_COLUMN = "column";

    /** 排序方式 */
    private static final String ORDER_TYPE = "order";

    private static final String ORDER_TYPE_ASC = "ASC";

    /** mysql 模糊查询之特殊字符下划线 （_、\） */
    public static final String LIKE_MYSQL_SPECIAL_STRS = "_,%";

    /** 日期格式化yyyy-MM-dd */
    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    /** to_date */
    public static final String TO_DATE = "to_date";

    /** 时间格式化 */
    private static final ThreadLocal<SimpleDateFormat> LOCAL = new ThreadLocal<>();

    private static SimpleDateFormat getTime() {
        SimpleDateFormat time = LOCAL.get();
        if (time == null) {
            time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            LOCAL.set(time);
        }
        return time;
    }

    /**
     * 获取查询条件构造器QueryWrapper实例 通用查询条件已被封装完成
     * @param searchObj 查询实体
     * @param parameterMap request.getParameterMap()
     * @return QueryWrapper实例
     */
    public static <T> QueryWrapper<T> initQueryWrapper(T searchObj, Map<String, String[]> parameterMap) {
        val stopWatch = new StopWatch(StrUtil.format("查询条件构造器({})计时器", searchObj.getClass()));
        stopWatch.start();
        QueryWrapper<T> queryWrapper = new QueryWrapper<T>();
        install(queryWrapper, searchObj, parameterMap);
        stopWatch.stop();
        log.trace("{}", stopWatch.shortSummary());
        return queryWrapper;
    }

    /**
     * 组装Mybatis Plus 查询条件
     */
    private static void install(QueryWrapper<?> queryWrapper, Object searchObj, Map<String, String[]> parameterMap) {
        val clazz = searchObj.getClass();

        // 区间条件组装 模糊查询 高级查询组装 简单排序
        PropertyDescriptor[] descriptors = BeanUtil.getPropertyDescriptors(clazz);
        String name, type, column;
        // 定义实体字段和数据库字段名称的映射 高级查询中 只能获取实体字段 如果设置TableField注解 那么查询条件会出问题
        Map<String, String> fieldColumnMap = new HashMap<>(descriptors.length);
        for (final PropertyDescriptor pd : descriptors) {
            name = pd.getName();
            type = pd.getPropertyType().toString();
            try {
                if (judgedIsUselessField(name) || pd.getReadMethod() == null
                        || !pd.getReadMethod().canAccess(searchObj)) {
                    continue;
                }
                column = getTableFieldName(clazz, name);
                if (column == null) {
                    // column为null只有一种情况 那就是 添加了注解@TableField(exist = false) 后续都不用处理了
                    continue;
                }

                Object value = BeanUtil.getFieldValue(searchObj, name);
                fieldColumnMap.put(name, column);

                // 区间查询
                doIntervalQuery(queryWrapper, parameterMap, type, name, column);
                // 判断单值 参数带不同标识字符串 走不同的查询
                // TODO 这种前后带逗号的支持分割后模糊查询(多选字段查询生效) 示例：,1,3,
                if (null != value && value.toString().startsWith(COMMA) && value.toString().endsWith(COMMA)) {
                    String values = value.toString().replace(",,", COMMA);
                    String[] vals = values.substring(1).split(COMMA);
                    final String field = StringUtils.camelToUnderline(column);
                    if (vals.length > 1) {
                        queryWrapper.and(j -> {
                            log.debug("查询过滤器({})，Query规则 --> field:{}, rule:{}, value:{}", clazz, field, "like",
                                    vals[0]);
                            j = j.like(field, vals[0]);
                            for (int k = 1; k < vals.length; k++) {
                                j = j.or().like(field, vals[k]);
                                log.debug("查询过滤器({})，Query规则 --> .or()---field:{}, rule:{}, value:{}", clazz, field,
                                        "like", vals[k]);
                            }
                        });
                    }
                    else {
                        log.debug("查询过滤器({})，Query规则 --> field:{}, rule:{}, value:{}", clazz, field, "like", vals[0]);
                        queryWrapper.and(j -> j.like(field, vals[0]));
                    }
                }
                else {
                    // 根据参数值带什么关键字符串判断走什么类型的查询
                    QueryRuleEnum rule = convert2Rule(value);
                    value = replaceValue(rule, value);
                    addEasyQuery(queryWrapper, column, rule, value);
                }

            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        // TBD：已使用自定义排序处理器
        // doMultiFieldsOrder(queryWrapper, parameterMap, fieldColumnMap, clazz);

        // 高级查询
        doSuperQuery(queryWrapper, parameterMap, fieldColumnMap, clazz);
    }

    /**
     * 区间查询
     * @param queryWrapper query对象
     * @param parameterMap 参数map
     * @param type 字段类型
     * @param filedName 字段名称
     * @param columnName 列名称
     */
    private static void doIntervalQuery(QueryWrapper<?> queryWrapper, Map<String, String[]> parameterMap, String type,
            String filedName, String columnName) throws ParseException {
        // 添加 判断是否有区间值
        String endValue = null, beginValue = null;
        if (parameterMap != null && parameterMap.containsKey(filedName + BEGIN)) {
            beginValue = parameterMap.get(filedName + BEGIN)[0].trim();
            addQueryByRule(queryWrapper, columnName, type, beginValue, QueryRuleEnum.GE);

        }
        if (parameterMap != null && parameterMap.containsKey(filedName + END)) {
            endValue = parameterMap.get(filedName + END)[0].trim();
            addQueryByRule(queryWrapper, columnName, type, endValue, QueryRuleEnum.LE);
        }
        // 多值查询
        if (parameterMap != null && parameterMap.containsKey(filedName + MULTI)) {
            endValue = parameterMap.get(filedName + MULTI)[0].trim();
            addQueryByRule(queryWrapper, columnName.replace(MULTI, ""), type, endValue, QueryRuleEnum.IN);
        }
    }

    private static void doMultiFieldsOrder(QueryWrapper<?> queryWrapper, Map<String, String[]> parameterMap,
            Map<String, String> fieldColumnMap, Class<?> clazz) {
        Set<String> allFields = fieldColumnMap.keySet();
        String column = null, order = null;
        if (parameterMap != null && parameterMap.containsKey(ORDER_COLUMN)) {
            column = parameterMap.get(ORDER_COLUMN)[0];
        }
        if (parameterMap != null && parameterMap.containsKey(ORDER_TYPE)) {
            order = parameterMap.get(ORDER_TYPE)[0];
        }
        if (isEmpty(column)) {
            return;
        }

        log.debug("排序规则({}) --> 列:{},排序方式:{}", clazz, column, order);

        if (DataBaseConstant.CREATE_TIME.equals(column) && !fieldColumnMap.containsKey(DataBaseConstant.CREATE_TIME)) {
            column = "id";
            log.warn("检测到实体里没有字段createTime，改成采用ID排序: {}！", clazz);
        }

        if (isNotEmpty(column) && isNotEmpty(order)) {
            // 判断column是不是当前实体的
            log.debug("排序规则({}) --> 可以排序字段有：{}", clazz, allFields);
            if (!allColumnExist(column, allFields)) {
                throw new ColumnNotFoundException(column, clazz.getSimpleName());
            }
            // 多字段排序方法没有读取 MybatisPlus 注解 @TableField/@TableId 里 value 的值
            if (column.contains(COMMA)) {
                List<String> columnList = Arrays.asList(column.split(COMMA));
                String columnStrNew = columnList.stream().map(fieldColumnMap::get).collect(Collectors.joining(COMMA));
                if (isNotEmpty(columnStrNew)) {
                    column = columnStrNew;
                }
            }
            else {
                column = fieldColumnMap.get(column);
            }

            // SQL注入check
            if (SqlInjectionUtils.check(column)) {
                throw new IllegalArgumentException(StrUtil.format("column '{}' of {} is illegal", column, clazz));
            }

            String columnStr = StringUtils.camelToUnderline(column);
            String[] columnArray = columnStr.split(",");

            if (order.toUpperCase().contains(ORDER_TYPE_ASC)) {
                queryWrapper.orderByAsc(Arrays.asList(columnArray));
            }
            else {
                queryWrapper.orderByDesc(Arrays.asList(columnArray));
            }
        }
    }

    /**
     * 多字段排序 判断所传字段是否存在
     */
    private static boolean allColumnExist(String columnStr, Set<String> allFields) {
        boolean exist = true;
        if (columnStr.contains(COMMA)) {
            String[] arr = columnStr.split(COMMA);
            for (String column : arr) {
                if (!allFields.contains(column)) {
                    exist = false;
                    break;
                }
            }
        }
        else {
            exist = allFields.contains(columnStr);
        }
        return exist;
    }

    /**
     * 高级查询
     * @param queryWrapper 查询对象
     * @param parameterMap 参数对象
     * @param fieldColumnMap 实体字段和数据库列对应的map
     * @param clazz 查询实体类
     */
    private static void doSuperQuery(QueryWrapper<?> queryWrapper, Map<String, String[]> parameterMap,
            Map<String, String> fieldColumnMap, Class<?> clazz) {
        if (parameterMap != null && parameterMap.containsKey(SUPER_QUERY_PARAMS)) {
            String superQueryParams = parameterMap.get(SUPER_QUERY_PARAMS)[0];
            String superQueryMatchType = parameterMap.get(SUPER_QUERY_MATCH_TYPE) != null
                    ? parameterMap.get(SUPER_QUERY_MATCH_TYPE)[0] : MatchTypeEnum.AND.getValue();
            MatchTypeEnum matchType = MatchTypeEnum.getByValue(superQueryMatchType);
            try {
                superQueryParams = URLDecoder.decode(superQueryParams, StandardCharsets.UTF_8);
                List<QueryCondition> conditions = JSON.readValue(superQueryParams,
                        new TypeReference<List<QueryCondition>>() {
                        });

                if (conditions == null || conditions.isEmpty()) {
                    return;
                }

                List<QueryCondition> filterConditions = conditions.stream()
                    .filter(rule -> isNotEmpty(rule.getField()) && isNotEmpty(rule.getRule())
                            && isNotEmpty(rule.getVal()))
                    .collect(Collectors.toList());
                if (filterConditions.isEmpty()) {
                    return;
                }
                // sql 拼接多余的 and
                log.debug("高级查询({})参数: {}", clazz, filterConditions);

                queryWrapper.and(andWrapper -> {
                    for (int i = 0; i < filterConditions.size(); i++) {
                        QueryCondition rule = filterConditions.get(i);
                        if (isNotEmpty(rule.getField()) && isNotEmpty(rule.getRule()) && isNotEmpty(rule.getVal())) {

                            log.debug("SuperQuery({}) --> {}", clazz, rule);

                            Object queryValue = rule.getVal();

                            if ("date".equals(rule.getType())) {
                                queryValue = DateUtil.parseDate(queryValue.toString());
                            }
                            else if ("datetime".equals(rule.getType())) {
                                queryValue = DateUtil.parseDateTime(queryValue.toString());
                            }

                            String dbType = rule.getDbType();
                            if (isNotEmpty(dbType)) {
                                try {
                                    String valueStr = String.valueOf(queryValue);
                                    switch (dbType.toLowerCase().trim()) {
                                        case "int":
                                            queryValue = Integer.parseInt(valueStr);
                                            break;
                                        case "bigdecimal":
                                            queryValue = new BigDecimal(valueStr);
                                            break;
                                        case "short":
                                            queryValue = Short.parseShort(valueStr);
                                            break;
                                        case "long":
                                            queryValue = Long.parseLong(valueStr);
                                            break;
                                        case "float":
                                            queryValue = Float.parseFloat(valueStr);
                                            break;
                                        case "double":
                                            queryValue = Double.parseDouble(valueStr);
                                            break;
                                        case "boolean":
                                            queryValue = Boolean.parseBoolean(valueStr);
                                            break;
                                        default:
                                    }
                                }
                                catch (Exception e) {
                                    log.error("高级查询值({})转换失败：{}", clazz, e.getMessage());
                                }
                            }
                            addEasyQuery(andWrapper, fieldColumnMap.get(rule.getField()),
                                    QueryRuleEnum.getByValue(rule.getRule()), queryValue);

                            // 如果拼接方式是OR，就拼接OR
                            if (MatchTypeEnum.OR == matchType && i < (filterConditions.size() - 1)) {
                                andWrapper.or();
                            }
                        }
                    }
                });
            }
            catch (Exception e) {
                log.error("高级查询({})拼接失败：{}", clazz.getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * 根据所传的值 转化成对应的比较方式 支持><= like in !
     */
    public static QueryRuleEnum convert2Rule(Object value) {
        if (value == null) {
            return QueryRuleEnum.EQ;
        }
        String val = value.toString().trim();
        if (val.isEmpty()) {
            return QueryRuleEnum.EQ;
        }
        QueryRuleEnum rule = null;

        // step 2 .>= =<
        int length2 = 2;
        int length3 = 3;
        if (val.length() >= length3) {
            if (QUERY_SEPARATE_KEYWORD.equals(val.substring(length2, length3))) {
                rule = QueryRuleEnum.getByValue(val.substring(0, 2));
            }
        }
        // step 1 .> <
        if (rule == null && val.length() >= length2) {
            if (QUERY_SEPARATE_KEYWORD.equals(val.substring(1, length2))) {
                rule = QueryRuleEnum.getByValue(val.substring(0, 1));
            }
        }

        // step 3 like
        if (rule == null && val.equals(STAR)) {
            rule = QueryRuleEnum.EQ;
        }
        if (rule == null && val.contains(STAR)) {
            if (val.startsWith(STAR) && val.endsWith(STAR)) {
                rule = QueryRuleEnum.LIKE;
            }
            else if (val.startsWith(STAR)) {
                rule = QueryRuleEnum.LEFT_LIKE;
            }
            else if (val.endsWith(STAR)) {
                rule = QueryRuleEnum.RIGHT_LIKE;
            }
        }

        // step 4 in
        if (rule == null && val.contains(COMMA)) {
            // TODO in 查询这里应该有个bug 如果一字段本身就是多选 此时用in查询 未必能查询出来
            rule = QueryRuleEnum.IN;
        }
        // step 5 !=
        if (rule == null && val.startsWith(NOT_EQUAL)) {
            rule = QueryRuleEnum.NE;
        }
        // step 6 xx+xx+xx 这种情况适用于如果想要用逗号作精确查询 但是系统默认逗号走in 所以可以用++替换【此逻辑作废】
        if (rule == null && val.indexOf(QUERY_COMMA_ESCAPE) > 0) {
            rule = QueryRuleEnum.EQ_WITH_ADD;
        }

        // 特殊处理：Oracle的表达式to_date('xxx','yyyy-MM-dd')含有逗号，会被识别为in查询，转为等于查询
        if (rule == QueryRuleEnum.IN && val.contains(YYYY_MM_DD) && val.contains(TO_DATE)) {
            rule = QueryRuleEnum.EQ;
        }

        return rule != null ? rule : QueryRuleEnum.EQ;
    }

    /**
     * 替换掉关键字字符
     */
    private static Object replaceValue(QueryRuleEnum rule, Object value) {
        if (rule == null) {
            return null;
        }
        if (!(value instanceof String)) {
            return value;
        }
        String val = value.toString().trim();
        if (QueryRuleEnum.EQ.getValue().equals(val)) {
            return val;
        }
        if (rule == QueryRuleEnum.LIKE) {
            value = val.substring(1, val.length() - 1);
            // mysql 模糊查询之特殊字符下划线 （_、\）
            value = specialStrConvert(value.toString());
        }
        else if (rule == QueryRuleEnum.LEFT_LIKE || rule == QueryRuleEnum.NE) {
            value = val.substring(1);
            // mysql 模糊查询之特殊字符下划线 （_、\）
            value = specialStrConvert(value.toString());
        }
        else if (rule == QueryRuleEnum.RIGHT_LIKE) {
            value = val.substring(0, val.length() - 1);
            // mysql 模糊查询之特殊字符下划线 （_、\）
            value = specialStrConvert(value.toString());
        }
        else if (rule == QueryRuleEnum.IN) {
            value = val.split(",");
        }
        else if (rule == QueryRuleEnum.EQ_WITH_ADD) {
            value = val.replaceAll("\\+\\+", COMMA);
        }
        else {
            if (val.startsWith(rule.getValue())) {
                // TODO !!!此处逻辑应该注释掉-> 如果查询内容中带有查询匹配规则符号，就会被截取的（比如：>=您好）!!
                value = val.replaceFirst(rule.getValue(), "");
            }
            else if (val.startsWith(rule.getCondition() + QUERY_SEPARATE_KEYWORD)) {
                value = val.replaceFirst(rule.getCondition() + QUERY_SEPARATE_KEYWORD, "").trim();
            }
        }
        return value;
    }

    private static void addQueryByRule(QueryWrapper<?> queryWrapper, String name, String type, String value,
            QueryRuleEnum rule) throws ParseException {
        if (isNotEmpty(value)) {
            // 针对数字类型字段，多值查询
            if (value.contains(COMMA)) {
                Object[] temp = Arrays.stream(value.split(COMMA)).map(v -> {
                    try {
                        return QueryGenerator.parseByType(v, type, rule);
                    }
                    catch (ParseException e) {
                        return v;
                    }
                }).toArray();
                addEasyQuery(queryWrapper, name, rule, temp);
                return;
            }
            Object temp = QueryGenerator.parseByType(value, type, rule);
            addEasyQuery(queryWrapper, name, rule, temp);
        }
    }

    /**
     * 根据类型转换给定的值
     */
    private static Object parseByType(String value, String type, QueryRuleEnum rule) throws ParseException {
        return switch (type) {
            case "class java.lang.Integer" -> Integer.parseInt(value);
            case "class java.math.BigDecimal" -> new BigDecimal(value);
            case "class java.lang.Short" -> Short.parseShort(value);
            case "class java.lang.Long" -> Long.parseLong(value);
            case "class java.lang.Float" -> Float.parseFloat(value);
            case "class java.lang.Double" -> Double.parseDouble(value);
            case "class java.util.Date" -> getDateQueryByRule(value, rule);
            default -> value;
        };
    }

    /**
     * 获取日期类型的值
     */
    private static Date getDateQueryByRule(String value, QueryRuleEnum rule) throws ParseException {
        Date date = null;
        int length = 10;
        if (value.length() == length) {
            if (rule == QueryRuleEnum.GE) {
                // 比较大于
                date = getTime().parse(value + " 00:00:00");
            }
            else if (rule == QueryRuleEnum.LE) {
                // 比较小于
                date = getTime().parse(value + " 23:59:59");
            }
            // TODO 日期类型比较特殊 可能oracle下不一定好使
        }
        if (date == null) {
            date = getTime().parse(value);
        }
        return date;
    }

    /**
     * 根据规则走不同的查询
     * @param queryWrapper QueryWrapper
     * @param name 字段名字
     * @param rule 查询规则
     * @param value 查询条件值
     */
    public static void addEasyQuery(QueryWrapper<?> queryWrapper, String name, QueryRuleEnum rule, Object value) {
        if (value == null || rule == null || isEmpty(value)) {
            return;
        }
        name = StringUtils.toUnderscore(name);
        log.debug("查询过滤器，Query规则 --> field:{}, rule:{}, value:{}", name, rule.getValue(), value);
        switch (rule) {
            case GT:
                queryWrapper.gt(name, value);
                break;
            case GE:
                queryWrapper.ge(name, value);
                break;
            case LT:
                queryWrapper.lt(name, value);
                break;
            case LE:
                queryWrapper.le(name, value);
                break;
            case EQ:
            case EQ_WITH_ADD:
                queryWrapper.eq(name, value);
                break;
            case NE:
                queryWrapper.ne(name, value);
                break;
            case IN:
                if (value instanceof String) {
                    queryWrapper.in(name, (Object[]) value.toString().split(COMMA));
                }
                else if (value instanceof String[]) {
                    queryWrapper.in(name, (Object[]) value);
                }
                else if (value.getClass().isArray()) {
                    queryWrapper.in(name, (Object[]) value);
                }
                else {
                    queryWrapper.in(name, value);
                }
                break;
            case LIKE:
                queryWrapper.like(name, value);
                break;
            case LEFT_LIKE:
                queryWrapper.likeLeft(name, value);
                break;
            case RIGHT_LIKE:
                queryWrapper.likeRight(name, value);
                break;
            default:
                log.warn("查询过滤器，Query规则未匹配 --> field:{}, rule:{}, value:{}", name, rule.getValue(), value);
                break;
        }
    }

    /**
     * 判断在生成查询条件时是否是要忽略的字段
     */
    private static boolean judgedIsUselessField(String name) {
        return "class".equals(name) || "ids".equals(name) || "page".equals(name) || "rows".equals(name)
                || "sort".equals(name) || "order".equals(name);
    }

    /**
     * 去掉值前后单引号
     */
    public static String trimSingleQuote(String ruleValue) {
        if (isEmpty(ruleValue)) {
            return "";
        }
        if (ruleValue.startsWith(QueryGenerator.SQL_SQ)) {
            ruleValue = ruleValue.substring(1);
        }
        if (ruleValue.endsWith(QueryGenerator.SQL_SQ)) {
            ruleValue = ruleValue.substring(0, ruleValue.length() - 1);
        }
        return ruleValue;
    }

    public static String getSqlRuleValue(String sqlRule) {
        try {
            Set<String> varParams = getSqlRuleParams(sqlRule);
            for (String var : varParams) {
                sqlRule = sqlRule.replace("#{" + var + "}", var);
            }
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return sqlRule;
    }

    /**
     * 获取sql中的#{key} 这个key组成的set
     */
    @Nonnull
    public static Set<String> getSqlRuleParams(String sql) {
        if (isEmpty(sql)) {
            return Collections.emptySet();
        }
        Set<String> varParams = new HashSet<>();
        String regex = "#\\{\\w+\\}";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String var = m.group();
            varParams.add(var.substring(var.indexOf("{") + 1, var.indexOf("}")));
        }
        return varParams;
    }

    /**
     * 获取查询条件
     */
    public static String getSingleQueryConditionSql(String field, String alias, Object value, boolean isString) {
        return getSingleQueryConditionSql(field, alias, value, isString, null);
    }

    /**
     * 报表获取查询条件 支持多数据源
     */
    public static String getSingleQueryConditionSql(String field, String alias, Object value, boolean isString,
            String dataBaseType) {
        if (value == null) {
            return "";
        }
        field = alias + StringUtils.toUnderscore(field);
        QueryRuleEnum rule = QueryGenerator.convert2Rule(value);
        return getSingleSqlByRule(rule, field, value, isString, dataBaseType);
    }

    /**
     * 获取单个查询条件的值
     * @param rule 条件比较运算
     * @param field 字段
     * @param value 值
     * @param isString string类型?
     * @param dataBaseType 数据库类型
     * @return 查询条件的值
     */
    private static String getSingleSqlByRule(QueryRuleEnum rule, String field, Object value, boolean isString,
            String dataBaseType) {
        return switch (rule) {
            case GT, GE, LT, LE, EQ -> field + rule.getValue() + getFieldConditionValue(value, isString, dataBaseType);
            case NE -> field + " <> " + getFieldConditionValue(value, isString, dataBaseType);
            case IN -> field + " in " + getInConditionValue(value, isString);
            case LIKE -> field + " like " + getLikeConditionValue(value, QueryRuleEnum.LIKE);
            case LEFT_LIKE -> field + " like " + getLikeConditionValue(value, QueryRuleEnum.LEFT_LIKE);
            case RIGHT_LIKE -> field + " like " + getLikeConditionValue(value, QueryRuleEnum.RIGHT_LIKE);
            default -> field + " = " + getFieldConditionValue(value, isString, dataBaseType);
        };
    }

    /**
     * 获取单个查询条件的值
     * @param rule 条件比较运算
     * @param field 字段
     * @param value 值
     * @param isString string?
     * @return 查询条件的值
     */
    private static String getSingleSqlByRule(QueryRuleEnum rule, String field, Object value, boolean isString) {
        return getSingleSqlByRule(rule, field, value, isString, null);
    }

    /**
     * 获取查询条件的值
     * @param value 值
     * @param isString string?
     * @param dataBaseType 数据库类型
     * @return 查询条件的值
     */
    private static String getFieldConditionValue(Object value, boolean isString, String dataBaseType) {
        String str = value.toString().trim();
        if (str.startsWith(SymbolConstant.EXCLAMATORY_MARK)) {
            str = str.substring(1);
        }
        else if (str.startsWith(QueryRuleEnum.GE.getValue())) {
            str = str.substring(2);
        }
        else if (str.startsWith(QueryRuleEnum.LE.getValue())) {
            str = str.substring(2);
        }
        else if (str.startsWith(QueryRuleEnum.GT.getValue())) {
            str = str.substring(1);
        }
        else if (str.startsWith(QueryRuleEnum.LT.getValue())) {
            str = str.substring(1);
        }
        else if (str.indexOf(QUERY_COMMA_ESCAPE) > 0) {
            str = str.replaceAll("\\+\\+", COMMA);
        }
        if (dataBaseType == null) {
            dataBaseType = getDbType();
        }
        if (isString) {
            if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(dataBaseType)) {
                return " N'" + str + "' ";
            }
            else {
                return " '" + str + "' ";
            }
        }
        else {
            // 如果不是字符串 有一种特殊情况 popup调用都走这个逻辑 参数传递的可能是“‘admin’”这种格式的
            if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(dataBaseType)
                    && str.endsWith(SymbolConstant.SINGLE_QUOTATION_MARK)
                    && str.startsWith(SymbolConstant.SINGLE_QUOTATION_MARK)) {
                return " N" + str;
            }
            return value.toString();
        }
    }

    private static String getInConditionValue(Object value, boolean isString) {
        String[] temp = value.toString().split(",");
        if (temp.length == 0) {
            return "('')";
        }
        if (isString) {
            List<String> res = new ArrayList<>();
            for (String string : temp) {
                if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                    res.add("N'" + string + "'");
                }
                else {
                    res.add("'" + string + "'");
                }
            }
            return "(" + String.join(",", res) + ")";
        }
        else {
            return "(" + value.toString() + ")";
        }
    }

    /**
     * 先根据值判断 走左模糊还是右模糊 最后如果值不带任何标识(*或者%)，则再根据ruleEnum判断
     */
    private static String getLikeConditionValue(Object value, QueryRuleEnum ruleEnum) {
        String str = value.toString().trim();
        if (str.startsWith(SymbolConstant.ASTERISK) && str.endsWith(SymbolConstant.ASTERISK)) {
            if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                return "N'%" + str.substring(1, str.length() - 1) + "%'";
            }
            else {
                return "'%" + str.substring(1, str.length() - 1) + "%'";
            }
        }
        else if (str.startsWith(SymbolConstant.ASTERISK)) {
            if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                return "N'%" + str.substring(1) + "'";
            }
            else {
                return "'%" + str.substring(1) + "'";
            }
        }
        else if (str.endsWith(SymbolConstant.ASTERISK)) {
            if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                return "N'" + str.substring(0, str.length() - 1) + "%'";
            }
            else {
                return "'" + str.substring(0, str.length() - 1) + "%'";
            }
        }
        else {
            if (str.contains(SymbolConstant.PERCENT_SIGN)) {
                boolean b = str.startsWith(SymbolConstant.SINGLE_QUOTATION_MARK)
                        && str.endsWith(SymbolConstant.SINGLE_QUOTATION_MARK);
                if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                    if (b) {
                        return "N" + str;
                    }
                    else {
                        return "N" + "'" + str + "'";
                    }
                }
                else {
                    if (b) {
                        return str;
                    }
                    else {
                        return "'" + str + "'";
                    }
                }
            }
            else {
                // 走到这里说明 value不带有任何模糊查询的标识(*或者%)
                if (ruleEnum == QueryRuleEnum.LEFT_LIKE) {
                    if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                        return "N'%" + str + "'";
                    }
                    else {
                        return "'%" + str + "'";
                    }
                }
                else if (ruleEnum == QueryRuleEnum.RIGHT_LIKE) {
                    if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                        return "N'" + str + "%'";
                    }
                    else {
                        return "'" + str + "%'";
                    }
                }
                else {
                    if (DataBaseConstant.DB_TYPE_SQLSERVER.equals(getDbType())) {
                        return "N'%" + str + "%'";
                    }
                    else {
                        return "'%" + str + "%'";
                    }
                }
            }
        }
    }

    /**
     * 转换sql中的系统变量
     */
    public static String convertSystemVariables(String sql) {
        return getSqlRuleValue(sql);
    }

    /**
     * 获取系统数据库类型
     */
    private static String getDbType() {
        return DataSourceUtils.getDatabaseType();
    }

    /**
     * 获取class的 包括父类的
     */
    private static List<Field> getClassFields(Class<?> clazz) {
        return BeanUtils.getAllFields(clazz);
    }

    /**
     * 获取表字段名
     */
    private static String getTableFieldName(Class<?> clazz, String name) {
        try {
            // 如果字段加注解了@TableField(exist = false),不走DB查询
            Field field = null;
            try {
                field = clazz.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored) {
            }

            // 如果为空，则去父类查找字段
            if (field == null) {
                List<Field> allFields = getClassFields(clazz);
                List<Field> searchFields = allFields.stream().filter(a -> a.getName().equals(name)).toList();
                if (!searchFields.isEmpty()) {
                    field = searchFields.get(0);
                }
            }

            if (field != null) {
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null) {
                    if (!tableField.exist()) {
                        // 如果设置了TableField false 这个字段不需要处理
                        return null;
                    }
                    else {
                        String column = tableField.value();
                        // 如果设置了TableField value 这个字段是实体字段
                        if (!"".equals(column)) {
                            return column;
                        }
                    }
                }
                else {
                    val tableId = field.getAnnotation(TableId.class);
                    if (tableId != null && org.apache.commons.lang3.StringUtils.isNotBlank(tableId.value())) {
                        return tableId.value();
                    }
                }
            }
        }
        catch (Exception ignored) {
        }
        return name;
    }

    /**
     * mysql 模糊查询之特殊字符下划线 （_、\）
     */
    private static String specialStrConvert(String value) {
        if (DataBaseConstant.DB_TYPE_MYSQL.equals(getDbType())
                || DataBaseConstant.DB_TYPE_MARIADB.equals(getDbType())) {
            String[] specialStr = QueryGenerator.LIKE_MYSQL_SPECIAL_STRS.split(",");
            for (String str : specialStr) {
                if (value.contains(str)) {
                    value = value.replace(str, "\\" + str);
                }
            }
        }
        return value;
    }

}
