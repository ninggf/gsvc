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
package com.apzda.cloud.gsvc.security.authentication;

import com.apzda.cloud.gsvc.dto.CurrentUser;
import lombok.val;

import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface AuthenticationDetails {

    String getDevice();

    String getDeviceId();

    String getOsName();

    String getOsVer();

    String getApp();

    String getRemoteAddress();

    Map<String, String> getAppMeta();

    default CurrentUser create(String uid) {
        return CurrentUser.builder()
            .uid(uid)
            .app(getApp())
            .device(getDevice())
            .deviceId(getDeviceId())
            .osVer(getOsVer())
            .os(getOsName())
            .meta(getAppMeta())
            .remoteAddress(getRemoteAddress())
            .build();
    }

    default GenericAuthenticationDetails generic() {
        if (this instanceof GenericAuthenticationDetails genericAuthenticationDetails) {
            return genericAuthenticationDetails;
        }
        val details = new GenericAuthenticationDetails();
        details.setApp(getApp());
        details.setAppMeta(getAppMeta());
        details.setDevice(getDevice());
        details.setDeviceId(getDeviceId());
        details.setOsVer(getOsVer());
        details.setOsName(getOsName());
        details.setRemoteAddress(getRemoteAddress());
        return details;
    }

}
