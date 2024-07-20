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
package com.apzda.cloud.gsvc.context.logging;

import lombok.val;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerGroup;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.log.LogMessage;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class GsvcLoggingListener implements GenericApplicationListener {

    private static final ConfigurationPropertyName LOGGING_LEVEL = ConfigurationPropertyName.of("logging.level");

    private static final Bindable<Map<String, LogLevel>> STRING_LOGLEVEL_MAP = Bindable.mapOf(String.class,
            LogLevel.class);

    private static final Class<?>[] EVENT_TYPES = { ApplicationEnvironmentPreparedEvent.class };

    private static final Class<?>[] SOURCE_TYPES = { SpringApplication.class, ApplicationContext.class };

    public static final Map<String, List<String>> DEFAULT_GROUP_LOGGERS = new HashMap<>();
    static {
        DEFAULT_GROUP_LOGGERS.compute("web", (key, values) -> {
            if (values == null) {
                values = new ArrayList<>();
            }
            values.add("com.apzda.cloud.gsvc.gtw");
            values.add("com.apzda.cloud.gsvc.core.WebclientFactoryBean");
            values.add("com.apzda.cloud.gsvc.core.GtwRouterFunctionFactoryBean");
            values.add("com.apzda.cloud.gsvc.core.GatewayServiceBeanFactoryPostProcessor");
            values.add("com.apzda.cloud.gsvc.security.config.GsvcSecurityAutoConfiguration");
            values.add("com.apzda.cloud.gsvc.security.config.GatewayAuthorizeCustomizer");
            values.add("com.apzda.cloud.gsvc.autoconfigure.GsvcWebMvcConfigure");
            values.add("com.apzda.cloud.gsvc.filter.GsvcServletFilter");

            return values;
        });
        DEFAULT_GROUP_LOGGERS.compute("sql", (key, values) -> {
            if (values == null) {
                values = new ArrayList<>();
            }
            values.add("com.apzda.cloud.gsvc.domain");
            values.add("com.apzda.cloud.boot.query");
            return values;
        });
    }
    private final Log logger = LogFactory.getLog(getClass());

    private LoggingSystem loggingSystem;

    private LoggerGroups loggerGroups;

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return isAssignableFrom(eventType.getRawClass(), EVENT_TYPES);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return isAssignableFrom(sourceType, SOURCE_TYPES);
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent preparedEvent) {
            SpringApplication springApplication = preparedEvent.getSpringApplication();
            if (this.loggingSystem == null) {
                this.loggingSystem = LoggingSystem.get(springApplication.getClassLoader());
            }
            this.loggerGroups = new LoggerGroups(DEFAULT_GROUP_LOGGERS);
            setLogLevels(loggingSystem, preparedEvent.getEnvironment());
        }

    }

    protected void setLogLevels(LoggingSystem system, ConfigurableEnvironment environment) {
        BiConsumer<String, LogLevel> customizer = getLogLevelConfigurer(system);
        Binder binder = Binder.get(environment);
        Map<String, LogLevel> levels = binder.bind(LOGGING_LEVEL, STRING_LOGLEVEL_MAP).orElseGet(Collections::emptyMap);
        String[] names = new String[] { "web", "sql" };
        for (String name : names) {
            val level = levels.get(name);
            if (level != null) {
                configureLogLevel(name, level, customizer);
            }
        }
    }

    private BiConsumer<String, LogLevel> getLogLevelConfigurer(LoggingSystem system) {
        return (name, level) -> {
            try {
                name = name.equalsIgnoreCase(LoggingSystem.ROOT_LOGGER_NAME) ? null : name;
                system.setLogLevel(name, level);
            }
            catch (RuntimeException ex) {
                this.logger.error(LogMessage.format("Cannot set level '%s' for '%s'", level, name));
            }
        };
    }

    private void configureLogLevel(String name, LogLevel level, BiConsumer<String, LogLevel> configurer) {
        if (this.loggerGroups != null) {
            LoggerGroup group = this.loggerGroups.get(name);
            if (group != null && group.hasMembers()) {
                group.configureLogLevel(level, configurer);
                return;
            }
        }
        configurer.accept(name, level);
    }

    private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
        if (type != null) {
            for (Class<?> supportedType : supportedTypes) {
                if (supportedType.isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

}
