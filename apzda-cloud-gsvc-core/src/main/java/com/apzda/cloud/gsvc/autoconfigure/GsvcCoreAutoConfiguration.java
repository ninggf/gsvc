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

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.i18n.LocaleResolverImpl;
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration(before = MessageSourceAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties
@ImportRuntimeHints(GsvcCoreAutoConfiguration.MessageSourceRuntimeHints.class)
public class GsvcCoreAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.messages")
    MessageSourceProperties messageSourceProperties() {
        return new MessageSourceProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    MessageSource messageSource(MessageSourceProperties properties,
            ObjectProvider<MessageSourceNameResolver> provider) {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

        val names = new HashSet<String>();

        for (MessageSourceNameResolver resolver : provider.orderedStream().toList()) {
            names.addAll(StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(resolver.baseNames())));
        }

        val baseNames = new ArrayList<>(names.stream().toList());
        if (StringUtils.hasText(properties.getBasename())) {
            baseNames.add(properties.getBasename());
        }

        properties.setBasename(String.join(",", baseNames));

        if (!baseNames.isEmpty()) {
            messageSource.setBasenames(baseNames.toArray(new String[0]));
        }

        if (properties.getEncoding() != null) {
            messageSource.setDefaultEncoding(properties.getEncoding().name());
        }
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        val cacheDuration = properties.getCacheDuration();
        if (cacheDuration != null) {
            messageSource.setCacheMillis(cacheDuration.toMillis());
        }
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
        return messageSource;
    }

    @Bean("core.MessageSourceNameResolver")
    MessageSourceNameResolver messageSourceNameResolver() {
        return () -> "messages-core";
    }

    @Bean
    @ConditionalOnBean({ MessageSource.class })
    I18nUtils i18nUtils() {
        return new I18nUtils() {
            private static final Logger log = LoggerFactory.getLogger(I18nUtils.class);

            @Override
            public void afterPropertiesSet() throws Exception {
                messageSource = applicationContext.getBean(MessageSource.class);
                try {
                    localeResolver = applicationContext.getBean(LocaleResolver.class);
                }
                catch (Exception ignored) {
                }
                I18nUtils.defaultLocale = applicationContext.getBean(Locale.class);
                log.trace("I18n Utils initialized: messageSource({}), localeResolver({})", messageSource,
                        localeResolver);
            }
        };
    }

    @Bean
    static GsvcContextHolder gsvcContextHolder() {
        return new GsvcContextHolder() {
        };
    }

    @Bean
    @ConditionalOnMissingBean
    LocaleResolver localeResolver(Locale defaultLocale) {
        return new LocaleResolverImpl("lang", defaultLocale);
    }

    @Bean
    @ConditionalOnMissingBean
    Locale defaultLocale() {
        return Locale.ENGLISH;
    }

    @Bean
    @ConditionalOnMissingBean
    Clock appClock() {
        return Clock.systemDefaultZone();
    }

    @Slf4j
    @Configuration(proxyBeanMethods = false)
    static class StaticConfigureConfiguration implements InitializingBean {

        @Value("${apzda.cloud.config.real-ip-header:X-Real-IP}")
        private String realIpHeader;

        @Value("${apzda.cloud.config.real-ip-from:}")
        private String realIpFrom;

        @Override
        public void afterPropertiesSet() throws Exception {
            ConfigureHelper.setRealIpHeader(realIpHeader);
            ConfigureHelper.setRealIpFrom(realIpFrom);
            if (StringUtils.hasText(realIpHeader) && !ConfigureHelper.getRealIpFrom().isEmpty()) {
                log.trace("Will try getting remote ip from header {} which sent by {}", realIpHeader,
                        ConfigureHelper.getRealIpFrom());
            }
        }

    }

    static class MessageSourceRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("messages.properties").registerPattern("messages_*.properties");
        }

    }

}
