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
package com.apzda.cloud.boot.autoconfig;

import com.apzda.cloud.boot.security.AclChecker;
import com.apzda.cloud.boot.security.NoChecker;
import com.apzda.cloud.boot.utils.DataSourceUtils;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import jakarta.annotation.Nonnull;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@ComponentScan({ "com.apzda.cloud.boot.aop" })
@MapperScan("com.apzda.cloud.boot.mapper")
@Import(SecurityAclCheckerConfiguration.class)
public class GsvcBootAutoConfiguration implements ApplicationContextAware {

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        DataSourceUtils.setDataSource(applicationContext.getBean(DataSource.class));
    }

    @Bean
    @ConditionalOnMissingBean
    AclChecker aclPermissionChecker() {
        return new NoChecker();
    }

}
