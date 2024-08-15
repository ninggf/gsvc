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
package com.apzda.cloud.demo.foo.config;

import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.bar.proto.SaService;
import com.apzda.cloud.demo.foo.proto.OrderService;
import com.apzda.cloud.demo.foo.security.FooLoginFilter;
import com.apzda.cloud.demo.math.proto.AccountService;
import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import com.apzda.cloud.gsvc.security.filter.AbstractProcessingFilter;
import com.apzda.cloud.gsvc.security.filter.SecurityFilterRegistrationBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@EnableGsvcServices({ BarService.class, SaService.class, OrderService.class, AccountService.class })
@MapperScan("com.apzda.cloud.demo.foo.domain.mapper")
@EnableMethodSecurity
public class FooConfiguration {

    @Bean("foo.MessageSourceNameResolver")
    MessageSourceNameResolver messageSourceNameResolver() {
        return () -> "messages-foo";
    }

    @Bean
    SecurityFilterRegistrationBean<AbstractProcessingFilter> fooLoginFilter() {
        return new SecurityFilterRegistrationBean<>(new FooLoginFilter());
    }

}
