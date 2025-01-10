/*
 * Copyright (C) 2023-2025 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.sharding.autoconfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.ProxyConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.datasource.pool.CatalogSwitchableDataSource;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereSchema;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rule.ShardingTable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(ShardingSphereConnection.class)
@EnableConfigurationProperties(ShardingConfigProperties.class)
public class ShardingSphereAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @RequiredArgsConstructor
    @Slf4j
    @ConditionalOnProperty(prefix = "apzda.cloud.shardingsphere.auto-create-table", name = "enabled",
            havingValue = "true")
    @ConditionalOnClass(DataSource.class)
    static class ShardingSphereServer implements SmartLifecycle {

        private final static HashMap<String, String> DDL = new HashMap<>();

        private final static HashMap<String, DataSource> DATASOURCE = new LinkedHashMap<>();

        private final static Pattern CREATE_TABLE_PATTERN = Pattern.compile(
                "^CREATE\\s+TABLE\\s+(`)?.+?(\\1)?(\\s+.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);

        private final static Pattern AUTO_PATTERN = Pattern.compile("AUTO_INCREMENT\\s*=\\s*\\d+",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);

        private final DataSource dataSource;

        private volatile boolean running = false;

        @Override
        public void start() {
            try {
                val connection = dataSource.getConnection();
                if (connection instanceof ProxyConnection proxyConnection
                        && proxyConnection.isWrapperFor(ShardingSphereConnection.class)) {
                    val conn = proxyConnection.unwrap(ShardingSphereConnection.class);
                    val contextManager = conn.getContextManager();
                    val metadata = contextManager.getMetaDataContexts();
                    val allTables = new ArrayList<>();
                    for (val each : metadata.getMetaData().getDatabases().entrySet()) {
                        val database = each.getValue();
                        val newTables = new HashSet<String>();
                        for (val rule : database.getRuleMetaData().getRules()) {
                            if (rule instanceof ShardingRule shardingRule) {
                                val tables = shardingRule.getShardingTables();
                                for (val table : tables.entrySet()) {
                                    newTables.addAll(autoCreateActualTable(database, table.getValue()));
                                }
                            }
                        }
                        if (!newTables.isEmpty()) {
                            for (Map.Entry<String, ShardingSphereSchema> entry : database.getSchemas().entrySet()) {
                                for (String ds : database.getResourceMetaData().getDataSourceMap().keySet()) {
                                    contextManager.reloadSchema(database, entry.getValue().getName(), ds);
                                    log.info("Reload schema '{}' of '{}' done!", entry.getValue().getName(), ds);
                                }
                            }
                            allTables.addAll(newTables);
                        }
                    }
                    log.info("Sharding Sphere Auto Create Tables count: {}", allTables.size());
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
            finally {
                for (Map.Entry<String, DataSource> entry : DATASOURCE.entrySet()) {
                    if (entry.getValue() instanceof HikariDataSource hikariDataSource) {
                        try {
                            hikariDataSource.close();
                        }
                        catch (Exception ignore) {
                        }
                    }
                }
            }
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        private DataSource getDataSource(ShardingSphereDatabase database, DataNode dn) {
            val storageUnit = database.getResourceMetaData().getStorageUnits().get(dn.getDataSourceName());
            if (storageUnit == null) {
                throw new RuntimeException("No such datasource: " + dn.getDataSourceName());
            }
            val ds = storageUnit.getDataSource();
            if (ds instanceof CatalogSwitchableDataSource csDataSource
                    && csDataSource.getDataSource() instanceof HikariDataSource hkDataSource) {
                val jdbcUrl = hkDataSource.getJdbcUrl();
                val username = hkDataSource.getUsername();
                val password = hkDataSource.getPassword();
                return DATASOURCE.computeIfAbsent(dn.getDataSourceName(), (k) -> {
                    val cfg = new HikariConfig();
                    cfg.setJdbcUrl(jdbcUrl);
                    cfg.setUsername(username);
                    cfg.setPassword(password);
                    cfg.setMaximumPoolSize(3);
                    cfg.setMinimumIdle(1);
                    cfg.setPoolName("TmpPool-" + k);
                    return new HikariDataSource(cfg);
                });
            }
            else {
                throw new RuntimeException("No such datasource: " + dn.getDataSourceName());
            }
        }

        private List<String> autoCreateActualTable(ShardingSphereDatabase database, ShardingTable shardingTable) {
            val logicTable = shardingTable.getLogicTable();
            val tables = new ArrayList<String>();
            for (DataNode dn : shardingTable.getActualDataNodes()) {
                val actualTable = dn.getTableName();
                val dataSource = getDataSource(database, dn);
                if (checkExists(actualTable, dataSource)) {
                    log.debug("Actual Table `{}` already exists", actualTable);
                    continue;
                }
                val sql = getSql(logicTable, dataSource);
                val realTableSql = AUTO_PATTERN
                    .matcher(CREATE_TABLE_PATTERN.matcher(sql.trim())
                        .replaceAll("CREATE TABLE IF NOT EXISTS `" + actualTable + "` $3"))
                    .replaceAll("");
                execDdl(realTableSql, dataSource);
                log.info("Actual Table `{}` created successfully", actualTable);
                tables.add(actualTable);
            }
            return tables;
        }

        private String getSql(String logicTable, DataSource dataSource) {
            return DDL.computeIfAbsent(logicTable, (k) -> {
                try (val connection = dataSource.getConnection()) {
                    val statement = connection.createStatement();
                    val result = statement.executeQuery("SHOW CREATE TABLE " + logicTable);
                    if (result != null && result.next()) {
                        return result.getString(2);
                    }
                    else {
                        throw new SQLException("'SHOW CREATE TABLE " + logicTable + "' failed");
                    }
                }
                catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        private void execDdl(String ddl, DataSource dataSource) {
            try (val connection = dataSource.getConnection()) {
                val statement = connection.createStatement();
                statement.execute(ddl);
            }
            catch (Exception e) {
                log.debug("Failed to execute DDL: {}", e.getMessage());
            }
        }

        private boolean checkExists(String actualTable, DataSource dataSource) {
            try (val connection = dataSource.getConnection()) {
                val statement = connection.createStatement();
                val result = statement.executeQuery("SHOW CREATE TABLE `" + actualTable + '`');
                return result != null && result.next();
            }
            catch (SQLException e) {
                return false;
            }
        }

    }

}
