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
package com.apzda.cloud.demo.foo.security;

import cn.hutool.core.bean.BeanUtil;
import com.apzda.cloud.gsvc.security.mfa.MfaStatus;
import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Component
public class FooAuthenticationCustomizer implements JwtTokenCustomizer {

    @Override
    @NonNull
    public JwtToken customize(@NonNull Authentication authentication, @NonNull JwtToken token) {
        val data = BeanUtil.copyProperties(token, FooLoginData.class);
        data.setUid(token.getName());
        if (authentication.getPrincipal() instanceof UserDetailsMeta udm) {
            data.setLastLoginTime(udm.get(UserDetailsMeta.LOGIN_TIME_META_KEY, authentication, 0L));
            if (udm.getUsername().equals("user1")) {
                udm.set(UserDetailsMeta.MFA_STATUS_KEY, authentication, MfaStatus.UNSET);
            }
        }
        return data;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FooLoginData implements JwtToken, Serializable {

        @Serial
        private static final long serialVersionUID = -2763131228048354173L;

        private String uid;

        private String name;

        private String provider;

        private String accessToken;

        private String refreshToken;

        private String mfa;

        private Long lastLoginTime;

        private String status;

        private boolean locked;

        private boolean credentialsExpired;

    }

}
