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
package com.apzda.cloud.gsvc.i18n;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class I18nHelper {

    private static MessageSource messageSource;

    private static LocaleResolver localeResolver;

    public I18nHelper(MessageSource messageSource, LocaleResolver localeResolver) {
        I18nHelper.messageSource = messageSource;
        I18nHelper.localeResolver = localeResolver;
    }

    public static String t(String code, Object[] args, String defaultString, Locale locale) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultString);
            return StringUtils.defaultString(defaultString, code);
        }
        return messageSource.getMessage(code, args, defaultString, locale);
    }

    public static String t(String code, String defaultString, Locale locale) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultString);
            return StringUtils.defaultString(defaultString, code);
        }
        return messageSource.getMessage(code, null, defaultString, locale);
    }

    public static String t(String code, String defaultString) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use default: '{}'", code, defaultString);
            return StringUtils.defaultString(defaultString, code);
        }
        val request = GsvcContextHolder.getRequest();
        if (request.isPresent()) {
            return messageSource.getMessage(code, null, defaultString, localeResolver.resolveLocale(request.get()));
        }
        else {
            return messageSource.getMessage(code, null, defaultString, Locale.getDefault());
        }
    }

    public static String t(String code) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use code as default: '{}'", code, code);
            return code;
        }
        val request = GsvcContextHolder.getRequest();
        if (request.isPresent()) {
            return messageSource.getMessage(code, null, null, localeResolver.resolveLocale(request.get()));
        }
        else {
            return messageSource.getMessage(code, null, null, Locale.getDefault());
        }
    }

    public static String t(String code, Object[] args) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use code as default: '{}'", code, code);
            return code;
        }

        val request = GsvcContextHolder.getRequest();

        return request
            .map(httpServletRequest -> messageSource.getMessage(code, args,
                    localeResolver.resolveLocale(httpServletRequest)))
            .orElseGet(() -> messageSource.getMessage(code, args, Locale.getDefault()));
    }

    public static String t(String code, Object[] args, Locale locale) {
        if (messageSource == null) {
            log.warn("Use I18nHelper too early to translate: '{}'. Use code as default: '{}'", code, code);
            return code;
        }

        return messageSource.getMessage(code, args, locale);
    }

}
