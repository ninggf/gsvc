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
package com.apzda.cloud.gsvc.security.resolver;

import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.security.authentication.DeviceAuthenticationDetails;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class CurrentUserParamResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.getParameter().getType().isAssignableFrom(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        val context = SecurityContextHolder.getContext();
        if (context == null) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
        val authentication = context.getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            val builder = getCurrentUserBuilder(authentication);
            return builder.build();
        }

        throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    }

    public static CurrentUser.CurrentUserBuilder getCurrentUserBuilder(Authentication authentication) {
        val builder = CurrentUser.builder();
        val uid = authentication.getName();
        builder.uid(uid);
        val details = authentication.getDetails();
        if (details instanceof DeviceAuthenticationDetails device) {
            builder.app(device.getApp());
            builder.os(device.getOsName());
            builder.osVer(device.getOsVer());
            builder.device(device.getDevice());
            builder.deviceId(device.getDeviceId());
            builder.meta(device.getAppMeta());
        }
        return builder;
    }
}
