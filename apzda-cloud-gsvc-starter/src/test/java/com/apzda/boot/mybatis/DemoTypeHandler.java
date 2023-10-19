/*
 * This file is part of apzda created at 2023/7/7 by ningGf.
 */
package com.apzda.boot.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created at 2023/7/7 16:34.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
public class DemoTypeHandler extends BaseTypeHandler<Double> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Double aDouble, JdbcType jdbcType) throws SQLException {
        preparedStatement.setDouble(i, aDouble * 100);
    }

    @Override
    public Double getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return resultSet.getDouble(s) / 100;
    }

    @Override
    public Double getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return resultSet.getDouble(i) / 100;
    }

    @Override
    public Double getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return callableStatement.getDouble(i) / 100;
    }
}
