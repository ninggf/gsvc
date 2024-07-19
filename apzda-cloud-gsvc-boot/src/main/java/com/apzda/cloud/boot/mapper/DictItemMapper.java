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
package com.apzda.cloud.boot.mapper;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Mapper
public interface DictItemMapper {

    @Select("SELECT ${labelField} from ${table} WHERE ${idField} = #{id}")
    String getDictItemLabel(String table, String idField, String id, String labelField);

    default String getDictLabel(String table, String idField, String labelField, String id) {

        if (SqlInjectionUtils.check(table)) {
            throw new MybatisPlusException("Discovering SQL injection table: " + table);
        }
        if (SqlInjectionUtils.check(idField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + idField);
        }
        if (SqlInjectionUtils.check(labelField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + labelField);
        }
        return getDictItemLabel(table, idField, id, labelField);
    }

    @Select("SELECT ${labelField} from ${table} WHERE ${codeField} = #{code} AND ${idField} = #{id}")
    String getTextFromDictItemTable(String table, String codeField, String code, String idField, String id,
            String labelField);

    default String getDictLabel(String table, String codeField, String code, String idField, String id,
            String labelField) {
        if (SqlInjectionUtils.check(table)) {
            throw new MybatisPlusException("Discovering SQL injection table: " + table);
        }
        if (SqlInjectionUtils.check(idField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + idField);
        }
        if (SqlInjectionUtils.check(codeField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + codeField);
        }
        if (SqlInjectionUtils.check(labelField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + labelField);
        }
        return getTextFromDictItemTable(table, codeField, code, idField, id, labelField);
    }

    @Select("SELECT ${labelField} from ${table} WHERE ${codeField} = #{code} AND ${idField} = #{id} AND ${delField} = #{delValue}")
    String getTextFromDictItemTable(String table, String codeField, String code, String idField, String id,
            String delField, String delValue, String labelField);

    default String getDictLabel(String table, String codeField, String code, String idField, String id, String delField,
            String delValue, String labelField) {
        if (SqlInjectionUtils.check(table)) {
            throw new MybatisPlusException("Discovering SQL injection table: " + table);
        }
        if (SqlInjectionUtils.check(idField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + idField);
        }
        if (SqlInjectionUtils.check(codeField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + codeField);
        }
        if (SqlInjectionUtils.check(labelField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + labelField);
        }
        if (SqlInjectionUtils.check(delField)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + delField);
        }

        return getTextFromDictItemTable(table, codeField, code, idField, id, delField, delValue, labelField);
    }

}
