package com.apzda.cloud.gsvc.security.token;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.exception.InvalidSessionException;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import java.util.Objects;

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
        val name = authentication.getName();
        token.setSubject(name);
        token.setSigner(jwtSigner);
        val accessExpireAt = DateUtil.date()
            .offset(DateField.MINUTE, (int) properties.getAccessTokenTimeout().toMinutes());

        token.setExpiresAt(accessExpireAt);

        val accessToken = token.sign();
        var refreshToken = createRefreshToken(accessToken, authentication);

        return JwtToken.builder().refreshToken(refreshToken).accessToken(accessToken).name(name).build();
    }

    @Override
    public JwtToken refreshAccessToken(@NonNull JwtToken jwtToken) {
        try {
            val name = jwtToken.getName();
            if (StringUtils.isBlank(name)) {
                throw new InsufficientAuthenticationException("username is empty");
            }

            val requestId = GsvcContextHolder.getRequestId();

            val refreshToken = jwtToken.getRefreshToken();
            if (StringUtils.isBlank(refreshToken)) {
                log.error("[{}] refreshToken is empty!", requestId);
                return null;
            }

            try {
                JWTUtil.verify(refreshToken, jwtSigner);
            }
            catch (Exception e) {
                log.error("[{}] refreshToken({}) is invalid: {}", requestId, refreshToken, e.getMessage());
                return null;
            }

            val jwt = JWTUtil.parseToken(refreshToken);
            jwt.setSigner(jwtSigner);

            val jwtLeeway = properties.getJwtLeeway();
            if (!jwt.validate(jwtLeeway.toSeconds())) {
                log.trace("[{}] refreshToken({}) is expired!", requestId, refreshToken);

                return null;
            }

            val userDetails = userDetailsService.loadUserByUsername(name);
            val authentication = JwtAuthenticationToken.unauthenticated(userDetailsMetaRepository.create(userDetails),
                    userDetails.getPassword());

            val accessToken = StringUtils.defaultString(jwtToken.getAccessToken());
            val password = userDetails.getPassword();
            val oldSign = (String) jwt.getPayload(JWT.SUBJECT);
            val sign = MD5.create().digestHex(accessToken + password);

            if (Objects.equals(oldSign, sign)) {
                return createJwtToken(authentication);
            }

            log.error("[{}] refreshToken({}) is invalid: accessToken or password does not match", requestId,
                    refreshToken);
        }
        catch (Exception e) {
            log.warn("[{}] Cannot refresh accessToken: {}", GsvcContextHolder.getRequestId(), e.getMessage());
        }

        return null;
    }

    @Override
    public String createRefreshToken(String accessToken, Authentication authentication) {
        val principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            val password = userDetails.getPassword();
            val expire = properties.getRefreshTokenTimeout();
            val accessExpireAt = DateUtil.date().offset(DateField.MINUTE, (int) expire.toMinutes());
            val token = JWT.create();
            val refreshToken = MD5.create().digestHex(accessToken + password);
            token.setSubject(refreshToken);
            token.setExpiresAt(accessExpireAt);
            token.setSigner(jwtSigner);
            return token.sign();
        }
        return "";
    }

    @Override
    public void verify(@NonNull Authentication authentication) throws SessionAuthenticationException {
        val requestId = GsvcContextHolder.getRequestId();

        if (authentication instanceof JwtAuthenticationToken auth) {
            if (auth.isLogin()) {
                return;
            }
            if (log.isTraceEnabled()) {
                log.trace("[{}] Current Session is not login", requestId);
            }
            throw new InvalidSessionException(String.format("[%s] Current Session is not login!", requestId));
        }

        if (log.isTraceEnabled()) {
            log.trace("[{}] Current Token is not supported: {}", requestId, authentication);
        }

        throw new InvalidSessionException(
                String.format("[%s] Authentication is not supported: %s", requestId, authentication));
    }

}
