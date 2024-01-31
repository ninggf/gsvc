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
import com.apzda.mybatis.plus.configure.MybatisCustomizer;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    // 增加的mybatis-plus配置
    @Bean
    ConfigurationCustomizer apzdaMybatisPlusConfigurationCustomizer() {
        return configuration -> {
            // 配置分页插件与乐观锁插件
            configuration.getInterceptors()
                .stream()
                .filter(interceptor -> interceptor instanceof MybatisPlusInterceptor)
                .findAny()
                .ifPresentOrElse(interceptor -> {
                    if (((MybatisPlusInterceptor) interceptor).getInterceptors()
                        .stream()
                        .filter(innerInterceptor -> innerInterceptor instanceof PaginationInnerInterceptor)
                        .findAny()
                        .isEmpty()) {
                        // log.debug("添加分页插件");
                        ((MybatisPlusInterceptor) interceptor).addInnerInterceptor(new PaginationInnerInterceptor());
                    }

                    if (((MybatisPlusInterceptor) interceptor).getInterceptors()
                        .stream()
                        .filter(innerInterceptor -> innerInterceptor instanceof OptimisticLockerInnerInterceptor)
                        .findAny()
                        .isEmpty()) {
                        // log.debug("添加乐观锁插件");
                        ((MybatisPlusInterceptor) interceptor)
                            .addInnerInterceptor(new OptimisticLockerInnerInterceptor());
                    }
                }, () -> {
                    // log.debug("配置分页与乐观锁插件");
                    val mybatisInterceptor = new MybatisPlusInterceptor();
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
    @ConditionalOnMissingBean
    MetaObjectHandler metaObjectHandler(CurrentUserProvider currentUserProvider) {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                val currentAuditor = currentUserProvider.getCurrentAuditor();
                val uid = currentAuditor.orElse(null);
                strictInsertFill(metaObject, "createdAt", Long.class, DateUtil.current());
                strictInsertFill(metaObject, "createdBy", String.class, uid);
                strictInsertFill(metaObject, "updatedAt", Long.class, DateUtil.current());
                strictInsertFill(metaObject, "updatedBy", String.class, uid);
                val tenantId = TenantManager.tenantId();
                strictInsertFill(metaObject, "tenantId", String.class, tenantId);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                val currentAuditor = currentUserProvider.getCurrentAuditor();
                val uid = currentAuditor.orElse(null);
                strictUpdateFill(metaObject, "updatedAt", Long.class, DateUtil.current());
                strictUpdateFill(metaObject, "updatedBy", String.class, uid);
            }
        };
    }

}
