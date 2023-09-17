package com.apzda.cloud.gsvc.security.token;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.JwtToken;
import com.apzda.cloud.gsvc.security.TokenManager;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author fengz
 */
@RequiredArgsConstructor
@Slf4j
public class JwtTokenManager implements TokenManager {

    protected final UserDetailsService userDetailsService;

    protected final UserDetailsMetaRepository userDetailsMetaRepository;

    protected final SecurityConfigProperties properties;

    protected final JWTSigner jwtSigner;

    @Override
    public Authentication restoreAuthentication(HttpServletRequest request) {
        val argName = properties.getArgName();
        val headerName = properties.getTokenName();
        val cookieConfig = properties.getCookie();
        val cookieName = cookieConfig.getCookieName();
        val bearer = properties.getBearer();
        val requestId = GsvcContextHolder.getRequestId();

        String accessToken = null;
        if (StringUtils.isNotBlank(argName)) {
            accessToken = StringUtils.defaultIfBlank(request.getParameter(argName), null);
            if (log.isTraceEnabled()) {
                log.trace("[{}] Try token from parameter({}): {}", requestId, argName, accessToken);
            }
        }

        val token = request.getHeader(headerName);
        if (StringUtils.isBlank(accessToken) && StringUtils.isNotBlank(token)) {
            if (StringUtils.isNotBlank(bearer) && token.startsWith(bearer)) {
                accessToken = token.substring(bearer.length() + 1);
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Try token from header({}: {}): {}", requestId, headerName, bearer, accessToken);
                }
            }
        }

        if (StringUtils.isBlank(accessToken) && StringUtils.isNotBlank(cookieName)) {
            accessToken = GsvcContextHolder.cookies().get(cookieName).getValue();
            if (log.isTraceEnabled()) {
                log.trace("[{}] Try token from cookie({}): {}", requestId, cookieName, accessToken);
            }
        }

        if (StringUtils.isNotBlank(accessToken)) {
            boolean verified = false;
            try {
                verified = JWTUtil.verify(accessToken, jwtSigner);
            }
            catch (Exception e) {
                if (log.isTraceEnabled()) {
                    log.trace("[{}] accessToken({}) is invalid: {}", requestId, accessToken, e.getMessage());
                }
            }

            if (verified) {
                val jwt = JWTUtil.parseToken(accessToken);
                jwt.setSigner(jwtSigner);
                val jwtLeeway = properties.getJwtLeeway();
                if (!jwt.validate(jwtLeeway.toSeconds())) {
                    log.trace("[{}] accessToken({}) is expired!", requestId, accessToken);
                    return null;
                }

                val jwtToken = JwtToken.builder()
                    .accessToken(accessToken)
                    .name((String) jwt.getPayload(JWT.SUBJECT))
                    .build();

                val userDetails = userDetailsService.loadUserByUsername(jwtToken.getName());
                if (userDetails == null) {
                    log.trace("[{}] accessToken({}) user details not found!", requestId, accessToken);
                    return null;
                }

                val authentication = JwtAuthenticationToken.authenticated(userDetailsMetaRepository.create(userDetails),
                        userDetails.getPassword());

                authentication.setJwtToken(jwtToken);

                return authentication;
            }
            else {
                log.trace("[{}] accessToken({}) is invalid", requestId, accessToken);
            }
        }
        else {
            log.trace("[{}] No token found", requestId);
        }

        return null;
    }

    @Override
    public JwtToken createJwtToken(Authentication authentication) {
        val token = JWT.create();
        val principal = authentication.getPrincipal();
        token.setSubject(authentication.getName());
        token.setSigner(jwtSigner);
        val accessExpireAt = DateUtil.date()
            .offset(DateField.MINUTE, (int) properties.getAccessTokenTimeout().toMinutes());
        token.setExpiresAt(accessExpireAt);

        val accessToken = token.sign();
        var refreshToken = createRefreshToken(authentication);

        val jwtToken = JwtToken.builder()
            .refreshToken(refreshToken)
            .accessToken(accessToken)
            .name(authentication.getName())
            .build();

        return jwtToken;
    }

    @Override
    public JwtToken refreshAccessToken(JwtToken token, Authentication authentication) {
        // todo refresh AccessToken
        return null;
    }

    @Override
    public String createRefreshToken(Authentication authentication) {
        // todo create RefreshToken
        return "";
    }

}
