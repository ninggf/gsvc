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
package com.apzda.boot;

import com.apzda.cloud.gsvc.mybatis.MybatisCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.Set;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration
public class MyBatisPlusConfig {

    @Bean
    MybatisCustomizer mybatisCustomizer() {
        return new MybatisCustomizer() {
            @Override
            public void addLocation(@NonNull Set<String> locations) {
                locations.add("classpath*:/com/apzda/**/*Mapper.xml");
            }

            @Override
            public void addTypeHandlersPackage(@NonNull Set<String> packages) {
                packages.add("com.apzda.boot.mybatis");
                packages.add("com.apzda.mybatis");
            }

            @Override
            public void addTenantIgnoreTable(@NonNull Set<String> tables) {
                tables.add("t_roles");
            }
        };
    }

}
