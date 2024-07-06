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
package com.apzda.cloud.gsvc.server;

import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

import java.lang.annotation.*;
import java.util.Arrays;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ConditionalOnLocalImpl.OnLocalImplementation.class)
public @interface ConditionalOnLocalImpl {

    Class<?> value();

    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    class OnLocalImplementation extends SpringBootCondition implements ConfigurationCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            if (metadata == null) {
                return ConditionOutcome.noMatch("ignore");
            }
            val ann = metadata.getAnnotations().get(ConditionalOnLocalImpl.class);
            val beanFactory = context.getBeanFactory();
            if (beanFactory == null) {
                return ConditionOutcome.noMatch("ignore");
            }
            val names = beanFactory.getBeanNamesForType(ann.synthesize().value());
            if (Arrays.stream(names)
                .anyMatch((name) -> StringUtils.startsWithAny(name, "gsvc", "grpc")
                        && StringUtils.endsWith(name, "Stub"))) {
                return ConditionOutcome.noMatch("");
            }
            else {
                return ConditionOutcome.match();
            }
        }

        @NonNull
        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }

    }

}
