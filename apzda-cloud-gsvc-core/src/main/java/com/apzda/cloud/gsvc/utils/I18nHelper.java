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
package com.apzda.cloud.gsvc.utils;

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.i18n.LocaleResolverImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class I18nHelper implements InitializingBean, ApplicationContextAware {

    private static MessageSource messageSource;

    private static LocaleResolver localeResolver;

    private static Locale defaultLocale;

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        I18nHelper.messageSource = applicationContext.getBean(MessageSource.class);
        try {
            I18nHelper.localeResolver = applicationContext.getBean(LocaleResolver.class);
        }
        catch (Exception ignored) {
        }
        I18nHelper.defaultLocale = applicationContext.getBean(Locale.class);
        log.debug("I18nHelper initialized with [{}, {}]", I18nHelper.messageSource, I18nHelper.localeResolver);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static String t(String code, Object[] args, String defaultStr, Locale locale) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultStr);
            return Objects.toString(defaultStr, code);
        }
        //@formatter:off
        val codeId = code.toLowerCase()
            .replace("{}", "0")
            .replaceAll("\\W+", ".");
        //@formatter:on
        try {
            val context = GsvcContextHolder.current();
            if (locale == null && context.getLocale() == null) {
                val userLocale = CurrentUserProvider.getCurrentUser().getLocale();
                locale = Optional.ofNullable(userLocale).orElse(defaultLocale);
                try {
                    val request = GsvcContextHolder.getRequest();
                    if (localeResolver instanceof LocaleResolverImpl localeResolver1) {
                        locale = localeResolver1.resolveLocale(request.orElse(null));
                    }
                    else {
                        locale = request.map(httpServletRequest -> localeResolver.resolveLocale(httpServletRequest))
                            .orElse(locale);
                    }
                }
                catch (Exception e) {
                    log.warn("Cannot detect Locale when translate: {} - {} - {}", codeId, args, e.getMessage());
                }
                context.setLocale(locale);
            }
            else if (locale == null) {
                locale = context.getLocale();
            }
            return messageSource.getMessage(codeId, args, defaultStr, locale);
        }
        catch (Exception e) {
            log.warn("Cannot translate '{}' with args: {} - {}", codeId, args, e.getMessage());
            return defaultStr;
        }
    }

    public static String t(String code, String defaultStr, Locale locale) {
        return t(code, null, defaultStr, locale);
    }

    public static String t(String code, String defaultStr) {
        return t(code, null, defaultStr, null);
    }

    public static String t(String code) {
        val defaultStr = code.replace("_", " ");
        return t(code, null, defaultStr, null);
    }

    public static String t(String code, Object[] args) {
        val defaultStr = MessageFormatter.arrayFormat(code.replace("_", " "), args).getMessage();
        return t(code, args, defaultStr);
    }

    public static String t(String code, Object[] args, Locale locale) {
        val defaultStr = MessageFormatter.arrayFormat(code.replace("_", " "), args).getMessage();
        return t(code, args, defaultStr, locale);
    }

    public static String t(String code, Object[] args, String defaultStr) {
        return t(code, args, defaultStr, null);
    }

    public static String t(MessageSourceResolvable resolvable) {
        return t(resolvable, null);
    }

    public static String t(MessageSourceResolvable resolvable, Locale locale) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", resolvable.getCodes(),
                    resolvable.getDefaultMessage());
            return resolvable.getDefaultMessage();
        }
        return messageSource.getMessage(resolvable, locale);
    }

}
