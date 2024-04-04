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

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class I18nHelper implements InitializingBean, ApplicationContextAware {

    private static MessageSource messageSource;

    private static LocaleResolver localeResolver;

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        I18nHelper.messageSource = applicationContext.getBean(MessageSource.class);
        I18nHelper.localeResolver = applicationContext.getBean(LocaleResolver.class);
        log.debug("I18nHelper initialized with [{}, {}]", I18nHelper.messageSource, I18nHelper.localeResolver);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static String t(String code, Object[] args, String defaultString, Locale locale) {
        code = code.replace(" ", "_");
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultString);
            return Objects.toString(defaultString, code);
        }
        return messageSource.getMessage(code, args, defaultString, locale);
    }

    public static String t(String code, String defaultString, Locale locale) {
        code = code.replace(" ", "_");
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultString);
            return Objects.toString(defaultString, code);
        }
        return messageSource.getMessage(code, null, defaultString, locale);
    }

    public static String t(String code, String defaultString) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultString);
            return Objects.toString(defaultString, code.replace("_", " "));
        }
        code = code.replace(" ", "_");
        val request = GsvcContextHolder.getRequest();
        if (request.isPresent()) {
            return messageSource.getMessage(code, null, defaultString, localeResolver.resolveLocale(request.get()));
        }
        else {
            return messageSource.getMessage(code, null, defaultString, Locale.getDefault());
        }
    }

    public static String t(String code) {
        code = code.replace(" ", "_");
        val defaultStr = code.replace("_", " ");
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use code as default: '{}'", code, defaultStr);
            return defaultStr;
        }
        val request = GsvcContextHolder.getRequest();
        if (request.isPresent()) {
            return messageSource.getMessage(code, null, defaultStr, localeResolver.resolveLocale(request.get()));
        }
        else {
            return messageSource.getMessage(code, null, defaultStr, Locale.getDefault());
        }
    }

    public static String t(String code, Object[] args) {
        if (messageSource == null) {
            val defaultStr = code.replace("_", " ");
            log.warn("Use I18nHelper too early to translate: '{}'. Use code as default: '{}'", code, defaultStr);
            return defaultStr;
        }
        val codeId = code.replace(" ", "_");
        val request = GsvcContextHolder.getRequest();
        return request
            .map(httpServletRequest -> messageSource.getMessage(codeId, args,
                    localeResolver.resolveLocale(httpServletRequest)))
            .orElseGet(() -> messageSource.getMessage(codeId, args, Locale.getDefault()));
    }

    public static String t(String code, Object[] args, String defaultStr) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultStr);
            return defaultStr;
        }

        val request = GsvcContextHolder.getRequest();

        return request
            .map(httpServletRequest -> messageSource.getMessage(code, args, defaultStr,
                    localeResolver.resolveLocale(httpServletRequest)))
            .orElseGet(() -> messageSource.getMessage(code, args, defaultStr, Locale.getDefault()));
    }

    public static String t(String code, Object[] args, Locale locale) {
        if (messageSource == null) {
            val defaultStr = code.replace("_", " ");
            log.warn("Use I18nHelper too early to translate: '{}'. Use code as default: '{}'", code, defaultStr);
            return defaultStr;
        }

        return messageSource.getMessage(code.replace(" ", "_"), args, locale);
    }

    public static String t(MessageSourceResolvable resolvable) {
        val request = GsvcContextHolder.getRequest();
        return request.map(httpServletRequest -> t(resolvable, localeResolver.resolveLocale(httpServletRequest)))
            .orElseGet(() -> t(resolvable, Locale.getDefault()));
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
