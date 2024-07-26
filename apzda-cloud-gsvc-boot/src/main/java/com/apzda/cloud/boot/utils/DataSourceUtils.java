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
package com.apzda.cloud.boot.utils;

import com.apzda.cloud.boot.query.DataBaseConstant;
import jakarta.annotation.Nonnull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public abstract class DataSourceUtils {

    private static String DB_TYPE = "";

    @Setter
    private static DataSource dataSource;

    public static String getDatabaseType() {
        if (dataSource == null) {
            return DB_TYPE;
        }
        return getDatabaseTypeByDataSource(dataSource);
    }

    /**
     * 获取数据库类型
     */
    public static String getDatabaseTypeByDataSource(@Nonnull DataSource dataSource) {
        if ("".equals(DB_TYPE)) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData md = connection.getMetaData();
                String dbType = md.getDatabaseProductName().toUpperCase();
                String sqlserver = "SQL SERVER";
                if (dbType.contains(DataBaseConstant.DB_TYPE_MYSQL)) {
                    DB_TYPE = DataBaseConstant.DB_TYPE_MYSQL;
                }
                else if (dbType.contains(DataBaseConstant.DB_TYPE_ORACLE)
                        || dbType.contains(DataBaseConstant.DB_TYPE_DM)) {
                    DB_TYPE = DataBaseConstant.DB_TYPE_ORACLE;
                }
                else if (dbType.contains(DataBaseConstant.DB_TYPE_SQLSERVER) || dbType.contains(sqlserver)) {
                    DB_TYPE = DataBaseConstant.DB_TYPE_SQLSERVER;
                }
                else if (dbType.contains(DataBaseConstant.DB_TYPE_POSTGRESQL)) {
                    DB_TYPE = DataBaseConstant.DB_TYPE_POSTGRESQL;
                }
                else if (dbType.contains(DataBaseConstant.DB_TYPE_MARIADB)) {
                    DB_TYPE = DataBaseConstant.DB_TYPE_MARIADB;
                }
                else {
                    log.error("数据库类型:[{}]不识别!", dbType);
                }
            }
            catch (Exception e) {
                log.error("获取数据库类型失败: {}", e.getMessage());
            }
        }
        return DB_TYPE;

    }

}
