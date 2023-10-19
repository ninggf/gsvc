/*
 * This file is part of apzda created at 2023/7/7 by ningGf.
 */
package com.apzda.mybatis;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.val;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created at 2023/7/7 16:06.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
public class CommaFieldTypeHandler extends BaseTypeHandler<List<String>> {
    private static final String SEP = "<->";

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, List<String> strings, JdbcType jdbcType) throws SQLException {
        val value = Joiner.on(SEP).skipNulls().join(strings);
        preparedStatement.setString(i, value);
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        final String value = resultSet.getString(s);
        return StringUtils.isBlank(value) ? null : parse(value);
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        final String value = resultSet.getString(i);
        return StringUtils.isBlank(value) ? null : parse(value);
    }

    @Override
    public List<String> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        final String value = callableStatement.getString(i);
        return StringUtils.isBlank(value) ? null : parse(value);
    }

    private List<String> parse(String value) {
        return Lists.newArrayList(Splitter.on(SEP).omitEmptyStrings().trimResults().split(value));
    }
}
