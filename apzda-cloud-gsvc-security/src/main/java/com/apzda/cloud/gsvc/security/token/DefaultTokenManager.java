package com.apzda.cloud.gsvc.security.token;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.IUser;
import com.apzda.cloud.gsvc.security.JwtToken;
import com.apzda.cloud.gsvc.security.TokenManager;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * @author fengz
 */
@RequiredArgsConstructor
public class DefaultTokenManager implements TokenManager {

    protected final SecurityConfigProperties properties;

    protected final JWTSigner jwtSigner;

    @Override
    public AuthenticationToken restore(HttpServletRequest request) {
        val argName = properties.getArgName();
        val tokenName = properties.getTokenName();
        val cookieConfig = properties.getCookie();
        val bearer = properties.getBearer();

        String accessToken = null;
        if (StringUtils.isNotBlank(argName)) {
            accessToken = StringUtils.defaultIfBlank(request.getParameter(argName), null);
        }

        val token = request.getHeader(tokenName);
        if (accessToken != null && StringUtils.isNotBlank(token)) {
            if (StringUtils.isNotBlank(bearer) && token.startsWith(bearer)) {
                accessToken = token.substring(bearer.length() + 1);
            }
        }

        if (StringUtils.isBlank(accessToken) && StringUtils.isNotBlank(cookieConfig.getCookieName())) {
            accessToken = GsvcContextHolder.cookies().get(cookieConfig.getCookieName()).getValue();
        }

        if (StringUtils.isNotBlank(accessToken)) {
            val verified = JWTUtil.verify(accessToken, jwtSigner);
            if (verified) {
                val jwt = JWTUtil.parseToken(accessToken);
                val uid = jwt.getPayload("uid");
                val jwtToken = JwtToken.builder()
                    .accessToken(accessToken)
                    .uid((String) uid)
                    .name((String) jwt.getPayload(JWT.SUBJECT))
                    .expireAt(DateUtil.parse(JWT.EXPIRES_AT))
                    .build();
            }
        }
        return null;
    }

    @Override
    public JwtToken createJwtToken(AuthenticationToken authentication) {
        val token = JWT.create();
        var uid = "0";
        if (authentication.getPrincipal() instanceof IUser user) {
            uid = user.getUid();
            token.setPayload("uid", uid);
        }
        token.setSubject(authentication.getName());
        token.setSigner(jwtSigner);
        val accessExpireAt = DateUtil.date()
            .offset(DateField.MINUTE, (int) properties.getAccessTokenTimeout().toMinutes());
        token.setExpiresAt(accessExpireAt);

        val accessToken = token.sign();
        var refreshToken = genRefreshToken(authentication);

        return JwtToken.builder()
            .refreshToken(refreshToken)
            .accessToken(accessToken)
            .uid(uid)
            .name(authentication.getName())
            .build();
    }

    @Override
    public JwtToken refresh(JwtToken token, AuthenticationToken authentication) {
        return createJwtToken(authentication);
    }

    protected String genRefreshToken(AuthenticationToken authentication) {
        return UUID.randomUUID().toString();
    }

}
