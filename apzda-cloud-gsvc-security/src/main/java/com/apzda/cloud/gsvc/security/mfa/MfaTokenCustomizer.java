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
package com.apzda.cloud.gsvc.security.mfa;

import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class MfaTokenCustomizer implements JwtTokenCustomizer {

    private final SecurityConfigProperties properties;

    @Override
    @NonNull
    public JwtToken customize(@NonNull Authentication authentication, @NonNull JwtToken token) {
        if (properties.isMfaEnabled() && token.getMfa() == null
                && authentication.getPrincipal() instanceof UserDetailsMeta userDetailsMeta) {
            token.setMfa(userDetailsMeta.get(UserDetailsMeta.MFA_STATUS_KEY, authentication, MfaStatus.DISABLED));
        }
        return token;
    }

}
