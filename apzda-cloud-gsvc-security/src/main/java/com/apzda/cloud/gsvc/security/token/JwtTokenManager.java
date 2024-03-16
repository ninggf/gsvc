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
import com.apzda.cloud.gsvc.security.exception.TokenException;
import com.apzda.cloud.gsvc.security.userdetails.CachedUserDetails;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private final ObjectProvider<List<JwtTokenCustomizer>> customizers;
    private String requestId;

    @Override
    public Authentication restoreAuthentication(HttpServletRequest request) {
        val argName = properties.getArgName();
        val headerName = properties.getTokenName();
        val cookieConfig = properties.getCookie();
        val cookieName = cookieConfig.getCookieName();
        val bearer = properties.getBearer();
        requestId = GsvcContextHolder.getRequestId();

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
            return restoreAuthentication(accessToken);
        }
        log.trace("[{}] No token found of request", requestId);
        return null;
    }

    @Override
    public Authentication restoreAuthentication(String accessToken) {
        boolean verified = false;
        try {
            verified = JWTUtil.verify(accessToken, jwtSigner);
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] accessToken({}) is invalid: {}", requestId, accessToken, e.getMessage());
            }
            throw TokenException.INVALID_TOKEN;
        }

        if (verified) {
            val jwt = JWTUtil.parseToken(accessToken);
            jwt.setSigner(jwtSigner);
            val jwtLeeway = properties.getJwtLeeway();
            if (!jwt.validate(jwtLeeway.toSeconds())) {
                log.trace("[{}] accessToken({}) is expired!", requestId, accessToken);
                throw TokenException.EXPIRED;
            }

            val jwtToken = SimpleJwtToken.builder()
                .accessToken(accessToken)
                .name((String) jwt.getPayload(JWT.SUBJECT))
                .build();
            val username = jwtToken.getName();

            val tmpUser = User.withUsername(username).password("").build();

            Optional<CachedUserDetails> cachedUserDetails = userDetailsMetaRepository.getMetaData(tmpUser,
                UserDetailsMeta.CACHED_USER_DETAILS_KEY,
                CachedUserDetails.class);

            val userDetails = cachedUserDetails.orElseGet(() -> {
                try {
                    log.trace("[{}] Try loading userDetails from userDetailsService: {}", requestId, username);
                    val ud = userDetailsService.loadUserByUsername(username);
                    if (ud != null) {
                        UserDetailsMeta.checkUserDetails(ud);
                        return CachedUserDetails.from(ud);
                    }
                } catch (Exception e) {
                    log.warn("[{}] Cannot load UserDetails from userDetailsService: {} - {}", requestId, username, e.getMessage());
                }
                return null;
            });

            if (userDetails == null) {
                log.trace("[{}] UserDetails of accessToken({}) not found!", requestId, accessToken);
                throw new UsernameNotFoundException(username + " not found!");
            }

            // 使用了空的authorities.
            val authentication = JwtAuthenticationToken.authenticated(userDetailsMetaRepository.create(userDetails),
                userDetails.getPassword());

            authentication.setJwtToken(jwtToken);
            log.trace("[{}] authentication is restored from accessToken({})", requestId, accessToken);
            return authentication;
        } else {
            log.trace("[{}] accessToken({}) is invalid", requestId, accessToken);
            throw TokenException.INVALID_TOKEN;
        }
    }

    @Override
    public void save(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            log.trace("[{}] Authentication Saved: {}", GsvcContextHolder.getRequestId(), authentication);
            userDetailsMetaRepository.setMetaData(userDetails,
                UserDetailsMeta.CACHED_USER_DETAILS_KEY,
                CachedUserDetails.from(userDetails)
            );
        }
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

        return SimpleJwtToken.builder().refreshToken(refreshToken).accessToken(accessToken).name(name).build();
    }

    @Override
    public JwtToken refreshAccessToken(@NonNull JwtToken jwtToken) {
        try {
            val requestId = GsvcContextHolder.getRequestId();
            val name = jwtToken.getName();
            if (StringUtils.isBlank(name)) {
                throw new InsufficientAuthenticationException("[" + requestId + "] username is empty");
            }

            val refreshToken = jwtToken.getRefreshToken();
            if (StringUtils.isBlank(refreshToken)) {
                log.error("[{}] refreshToken is empty!", requestId);
                return null;
            }

            try {
                JWTUtil.verify(refreshToken, jwtSigner);
            } catch (Exception e) {
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

            UserDetailsMeta.checkUserDetails(userDetails);

            val accessToken = StringUtils.defaultString(jwtToken.getAccessToken());
            val password = userDetails.getPassword();
            val oldSign = (String) jwt.getPayload(JWT.SUBJECT);
            val sign = MD5.create().digestHex(accessToken + password);

            if (Objects.equals(oldSign, sign)) {
                val authentication = JwtAuthenticationToken.unauthenticated(userDetailsMetaRepository.create(userDetails),
                    userDetails.getPassword());

                authentication.setJwtToken(jwtToken);

                if (!authentication.isLogin()) {
                    throw new InvalidSessionException("[" + requestId + "] Not Login");
                }

                val newJwtToken = createJwtToken(authentication);
                authentication.login(newJwtToken);
                save(authentication, GsvcContextHolder.getRequest().orElse(null));

                JwtToken newToken = newJwtToken;
                val cs = customizers.getIfAvailable();
                if (cs != null) {
                    for (JwtTokenCustomizer c : cs) {
                        newToken = c.customize(authentication, newToken);
                    }
                }

                return newToken;
            }

            log.error("[{}] refreshToken({}) is invalid: accessToken or password does not match", requestId,
                refreshToken);
        } catch (Exception e) {
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
            if (auth.getJwtToken() == null || auth.isLogin()) {
                return;
            }
            if (log.isTraceEnabled()) {
                log.trace("[{}] Current Session is not login", requestId);
            }
            throw new InvalidSessionException("Not login");
        }

        if (log.isTraceEnabled()) {
            log.trace("[{}] Current Token is not supported: {}", requestId, authentication);
        }

        throw new InvalidSessionException("Not Support");
    }

}
