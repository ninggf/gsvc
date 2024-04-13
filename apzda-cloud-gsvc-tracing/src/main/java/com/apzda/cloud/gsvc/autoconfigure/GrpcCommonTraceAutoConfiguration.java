/*
 * Copyright (C) 2023 Fengz Ning (windywany@gmail.com)
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

import brave.Tracing;
import brave.grpc.GrpcTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration(after = BraveAutoConfiguration.class)
@ConditionalOnClass({ Tracing.class, GrpcTracing.class })
@ConditionalOnBean(Tracing.class)
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true")
public class GrpcCommonTraceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GrpcCommonTraceAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public GrpcTracing grpcTracing(final Tracing tracing) {
        log.trace("GrpcTracing created");
        return GrpcTracing.create(tracing);
    }

}
