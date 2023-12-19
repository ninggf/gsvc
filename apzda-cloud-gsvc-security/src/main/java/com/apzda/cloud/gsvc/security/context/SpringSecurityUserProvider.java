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
package com.apzda.cloud.gsvc.security.context;

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.security.authentication.DeviceAuthenticationDetails;
import lombok.val;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class SpringSecurityUserProvider extends CurrentUserProvider {

    @Override
    protected CurrentUser currentUser() {
        val context = SecurityContextHolder.getContext();
        if (context == null) {
            return null;
        }

        val authentication = context.getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

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
        }

        return builder.build();
    }

}
