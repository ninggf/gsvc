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
package com.apzda.cloud.demo.bar.config;

import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.bar.proto.FileService;
import com.apzda.cloud.demo.bar.proto.SaService;
import com.apzda.cloud.demo.bar.proto.StorageService;
import com.apzda.cloud.demo.foo.proto.OrderService;
import com.apzda.cloud.demo.math.proto.MathService;
import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@EnableGsvcServices({ BarService.class, SaService.class, FileService.class, MathService.class, StorageService.class,
        OrderService.class })
@MapperScan("com.apzda.cloud.demo.bar.domain.mapper")
public class BarConfiguration {

    @Bean("bar.MessageSourceNameResolver")
    MessageSourceNameResolver messageSourceNameResolver() {
        return () -> "messages-bar";
    }

    @Bean
    @Order(1)
    JwtTokenCustomizer customizer1() {
        return (authentication, token) -> {
            token.setStatus("1");
            return token;
        };
    }

    @Bean
    @Order(2)
    JwtTokenCustomizer customizer2() {
        return (authentication, token) -> {
            token.setStatus("2");
            if (!"user5".equals(token.getName())) {
                token.setUid(token.getName());
            }
            return token;
        };
    }

}
