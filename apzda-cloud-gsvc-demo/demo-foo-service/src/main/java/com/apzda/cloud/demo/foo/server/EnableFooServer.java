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
package com.apzda.cloud.demo.foo.server;

import com.apzda.cloud.demo.bar.proto.BarServiceGsvc;
import com.apzda.cloud.demo.bar.proto.SaServiceGsvc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.lang.annotation.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ BarServiceGsvc.class, SaServiceGsvc.class })
@PropertySource({ "classpath:FooService.gateway.properties" })
@ComponentScan(basePackages = { "com.apzda.cloud.demo.foo" },
        excludeFilters = @ComponentScan.Filter(classes = SpringBootApplication.class))
@EnableMethodSecurity
@Documented
public @interface EnableFooServer {

}
