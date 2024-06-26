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

import com.apzda.cloud.demo.foo.security.FooLoginFilter;
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import com.apzda.cloud.gsvc.security.filter.AbstractProcessingFilter;
import com.apzda.cloud.gsvc.security.filter.SecurityFilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
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
