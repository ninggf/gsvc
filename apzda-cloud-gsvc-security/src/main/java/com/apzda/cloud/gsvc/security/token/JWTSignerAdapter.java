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
package com.apzda.cloud.gsvc.security.token;

import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import lombok.val;
import org.springframework.util.Assert;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class JWTSignerAdapter implements JWTSigner {

    private final static ThreadLocal<JWTSigner> jwtSigners = new ThreadLocal<>();

    private final SecurityConfigProperties properties;

    public JWTSignerAdapter(SecurityConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sign(String headerBase64, String payloadBase64) {
        return getJwtSigner().sign(headerBase64, payloadBase64);
    }

    @Override
    public boolean verify(String headerBase64, String payloadBase64, String signBase64) {
        return getJwtSigner().verify(headerBase64, payloadBase64, signBase64);
    }

    @Override
    public String getAlgorithm() {
        return getJwtSigner().getAlgorithm();
    }

    JWTSigner getJwtSigner() {
        var signer = jwtSigners.get();

        if (signer == null) {
            val jwtKey = properties.getJwtKey();
            Assert.hasText(jwtKey, "apzda.cloud.security.jwt-key is not set");
            signer = JWTSignerUtil.hs256(jwtKey.getBytes());
            jwtSigners.set(signer);
        }

        return signer;
    }

}
