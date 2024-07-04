/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.autoconfigure;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.mybatis.MybatisCustomizer;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.handlers.StrictFill;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.apzda.cloud.gsvc.utils.SnowflakeUtil.SNOWFLAKE;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@AutoConfiguration(before = MybatisPlusAutoConfiguration.class)
@ConditionalOnClass(MybatisConfiguration.class)
public class MyBatisPlusAutoConfiguration {

    private static final Pattern PATTERN = Pattern.compile("_([a-z])");

    // 增加的mybatis-plus配置
    @Bean
    ConfigurationCustomizer apzdaMybatisPlusConfigurationCustomizer(
            final ObjectProvider<TenantManager<?>> tenantManager,
            final ObjectProvider<List<MybatisCustomizer>> customizers) {
        val ignoreTables = new HashSet<String>();

        customizers.ifAvailable((customizersLst) -> {
            for (MybatisCustomizer customizer : customizersLst) {
                customizer.addTenantIgnoreTable(ignoreTables);
            }
        });

        return configuration -> {
            // 配置分页插件与乐观锁插件
            configuration.getInterceptors()
                .stream()
                .filter(interceptor -> interceptor instanceof MybatisPlusInterceptor)
                .findAny()
                .ifPresentOrElse(interceptor -> {
                    val mybatisPlusInterceptor = (MybatisPlusInterceptor) interceptor;
                    tenantManager.ifAvailable(tm -> {
                        if (tm.disableTenantPlugin()) {
                            return;
                        }
                        val tenantIdColumn = org.apache.commons.lang3.StringUtils.defaultIfBlank(tm.getTenantIdColumn(),
                                "tenant_id");
                        mybatisPlusInterceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                                new DefaultTenantLineHandler(tenantIdColumn, ignoreTables)));
                    });

                    if (mybatisPlusInterceptor.getInterceptors()
                        .stream()
                        .filter(innerInterceptor -> innerInterceptor instanceof PaginationInnerInterceptor)
                        .findAny()
                        .isEmpty()) {
                        // log.debug("添加分页插件");
                        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
                    }

                    if (mybatisPlusInterceptor.getInterceptors()
                        .stream()
                        .filter(innerInterceptor -> innerInterceptor instanceof OptimisticLockerInnerInterceptor)
                        .findAny()
                        .isEmpty()) {
                        // log.debug("添加乐观锁插件");
                        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
                    }
                }, () -> {
                    // log.debug("配置分页与乐观锁插件");
                    val mybatisInterceptor = new MybatisPlusInterceptor();
                    tenantManager.ifAvailable(tm -> {
                        if (tm.disableTenantPlugin()) {
                            return;
                        }
                        val tenantIdColumn = org.apache.commons.lang3.StringUtils.defaultIfBlank(tm.getTenantIdColumn(),
                                "tenant_id");
                        mybatisInterceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                                new DefaultTenantLineHandler(tenantIdColumn, ignoreTables)));
                    });
                    mybatisInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
                    mybatisInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
                    configuration.addInterceptor(mybatisInterceptor);
                });
        };
    }

    @Bean
    MybatisPlusPropertiesCustomizer apzdaMybatisPlusPropertiesCustomizer(
            final ObjectProvider<List<MybatisCustomizer>> customizers) {
        return properties -> {
            var mapperLocations = Optional.ofNullable(properties.getMapperLocations())
                .orElse(new String[] { "classpath*:/mapper/**/*Mapper.xml" });

            final Set<String> locations = new HashSet<>(List.of(mapperLocations));

            customizers.ifAvailable((customizerList) -> {
                for (MybatisCustomizer customizer : customizerList) {
                    customizer.addLocation(locations);
                }
            });

            log.debug("Mapper File Locations: {}", locations);
            String[] locs = new String[locations.size()];
            locs = locations.toArray(locs);
            properties.setMapperLocations(locs);

            // typeHandlersPackage
            val typeHandlersPackage = properties.getTypeHandlersPackage();
            final Set<String> packages = new HashSet<>();

            customizers.ifAvailable((customizerList) -> {
                for (MybatisCustomizer customizer : customizerList) {
                    customizer.addTypeHandlersPackage(packages);
                }
            });

            if (!CollectionUtils.isEmpty(packages)) {
                if (StringUtils.hasText(typeHandlersPackage)) {
                    properties.setTypeHandlersPackage(typeHandlersPackage + ";" + Joiner.on(";").join(packages));
                }
                else {
                    properties.setTypeHandlersPackage(Joiner.on(";").join(packages));
                }
            }
            log.debug("TypeHandlers Packages: {}", properties.getTypeHandlersPackage());
        };
    }

    @Bean
    @ConditionalOnMissingBean
    IdentifierGenerator idGenerator() {
        return entity -> SNOWFLAKE.nextId();
    }

    @Bean
    @ConditionalOnBean(CurrentUserProvider.class)
    @ConditionalOnMissingBean
    MetaObjectHandler metaObjectHandler(CurrentUserProvider currentUserProvider,
            ObjectProvider<TenantManager<?>> tenantManagers) {
        val stringBuffer = new StringBuffer();
        tenantManagers.ifAvailable(tenantManager -> {
            val tenantIdColumn = org.apache.commons.lang3.StringUtils.defaultIfBlank(tenantManager.getTenantIdColumn(),
                    "tenant_id");
            stringBuffer.append(PATTERN.matcher(tenantIdColumn).replaceAll(m -> m.group(1).toUpperCase()));
        });
        val tenantIdColumn = org.apache.commons.lang3.StringUtils.defaultIfBlank(stringBuffer.toString(), "tenantId");
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                val fills = new ArrayList<StrictFill<?, ?>>();
                val timeType = metaObject.getGetterType("createdAt");
                if (timeType != null) {
                    val ctime = getTime("createdAt", timeType);
                    fills.add(ctime);
                    fills.add(ctime.changeFieldName("updatedAt"));
                }

                val currentAuditor = currentUserProvider.getCurrentAuditor();
                val uidType = metaObject.getGetterType("createdBy");
                if (uidType != null && currentAuditor.isPresent()
                        && org.apache.commons.lang3.StringUtils.isNotBlank(currentAuditor.get())) {
                    val uid = getUid("createdBy", uidType, currentAuditor.get());
                    fills.add(uid);
                    fills.add(uid.changeFieldName("updatedBy"));
                }

                if (!fills.isEmpty()) {
                    strictInsertFill(findTableInfo(metaObject), metaObject, fills);
                }

                val idType = TenantManager.getIdType();
                val tenantId = TenantManager.tenantId();
                if (idType != null && tenantId != null && StringUtils.hasText(tenantId.toString())) {
                    if (idType.isAssignableFrom(Long.class)) {
                        strictInsertFill(metaObject, tenantIdColumn, Long.class, (Long) tenantId);
                    }
                    else {
                        strictInsertFill(metaObject, tenantIdColumn, String.class, tenantId.toString());
                    }
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                val fills = new ArrayList<StrictFill<?, ?>>();
                val timeType = metaObject.getGetterType("createdAt");
                if (timeType != null) {
                    val ctime = getTime("updatedAt", timeType);
                    fills.add(ctime);
                }
                val currentAuditor = currentUserProvider.getCurrentAuditor();
                val uidType = metaObject.getGetterType("updatedBy");
                if (uidType != null && currentAuditor.isPresent()
                        && org.apache.commons.lang3.StringUtils.isNotBlank(currentAuditor.get())) {
                    val uid = getUid("updatedBy", uidType, currentAuditor.get());
                    fills.add(uid);
                }

                if (!fills.isEmpty()) {
                    strictUpdateFill(findTableInfo(metaObject), metaObject, fills);
                }
            }

            private ClonableStrictFill<?, ?> getUid(String name, Class<?> userIdClz, String userId) {
                ClonableStrictFill<?, ?> uid;
                if (userIdClz == null) {
                    uid = new ClonableStrictFill<>(name, String.class, () -> null);
                }
                else if (Long.class.isAssignableFrom(userIdClz)) {
                    uid = new ClonableStrictFill<>(name, Long.class, () -> Long.parseLong(userId));
                }
                else if (Integer.class.isAssignableFrom(userIdClz)) {
                    uid = new ClonableStrictFill<>(name, Integer.class, () -> Integer.parseInt(userId));
                }
                else if (org.apache.commons.lang3.StringUtils.isNotBlank(userId)) {
                    uid = new ClonableStrictFill<>(name, String.class, () -> userId);
                }
                else {
                    uid = new ClonableStrictFill<>(name, String.class, () -> null);
                }
                return uid;
            }

            private ClonableStrictFill<?, ?> getTime(String name, Class<?> timeType) {
                ClonableStrictFill<?, ?> current;
                if (timeType == null || Long.class.isAssignableFrom(timeType)) {
                    current = new ClonableStrictFill<>(name, Long.class, System::currentTimeMillis);
                }
                else if (Date.class.isAssignableFrom(timeType)) {
                    current = new ClonableStrictFill<>(name, Date.class, Date::new);
                }
                else if (LocalDate.class.isAssignableFrom(timeType)) {
                    current = new ClonableStrictFill<>(name, LocalDate.class, LocalDate::now);
                }
                else if (LocalDateTime.class.isAssignableFrom(timeType)) {
                    current = new ClonableStrictFill<>(name, LocalDateTime.class, LocalDateTime::now);
                }
                else {
                    current = new ClonableStrictFill<>(name, String.class, DateUtil::now);
                }
                return current;
            }
        };
    }

    private record DefaultTenantLineHandler(String tenantIdColumn,
            Set<String> ignoreTables) implements TenantLineHandler {
        @Override
        public Expression getTenantId() {
            val idType = TenantManager.getIdType();

            if (idType == null) {
                return new NullValue();
            }

            Object tenantId = TenantManager.tenantId();

            if (Objects.isNull(tenantId) || org.apache.commons.lang3.StringUtils.isBlank(tenantId.toString())) {
                return new NullValue();
            }

            if (idType.isAssignableFrom(Long.class)) {
                return new LongValue(String.valueOf(tenantId));
            }

            return new StringValue((String) tenantId);
        }

        @Override
        public boolean ignoreTable(String tableName) {
            return ignoreTables.contains(tableName);
        }

        @Override
        public String getTenantIdColumn() {
            return tenantIdColumn;
        }

    }

    static class ClonableStrictFill<T, E extends T> extends StrictFill<T, E> {

        public ClonableStrictFill(String fieldName, Class<T> fieldType, Supplier<E> fieldVal) {
            super(fieldName, fieldType, fieldVal);
        }

        public ClonableStrictFill<T, E> changeFieldName(String name) {
            return new ClonableStrictFill<>(name, this.getFieldType(), this.getFieldVal());
        }

    }

}
