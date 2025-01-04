package com.apzda.cloud.gsvc.security.token;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.Mode;
import com.apzda.cloud.gsvc.security.authentication.AuthenticationDetails;
import com.apzda.cloud.gsvc.security.authentication.DeviceAuthenticationDetails;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.exception.InvalidSessionException;
import com.apzda.cloud.gsvc.security.exception.TokenException;
import com.apzda.cloud.gsvc.security.userdetails.CachedUserDetails;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Objects;

/**
 * @author fengz
 */
@RequiredArgsConstructor
@Slf4j
public class JwtTokenManager implements TokenManager, EnvironmentAware {

    private final static String PAYLOAD_UID = "uid";

    private final static String PAYLOAD_PD = "pd";

    private final static String PAYLOAD_RUNAS = "rs";

    private final static String PAYLOAD_DETAIL = "data";

    protected final UserDetailsService userDetailsService;

    protected final UserDetailsMetaRepository userDetailsMetaRepository;

    protected final SecurityConfigProperties properties;

    protected final JWTSigner jwtSigner;

    private final ObjectProvider<JwtTokenCustomizer> customizers;

    private final ObjectMapper objectMapper;

    private boolean validateAccessToken;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.validateAccessToken = environment.getProperty("apzda.cloud.gateway.default.enabled", Boolean.class, false)
                || environment.getProperty("apzda.cloud.config.mode", Mode.class, Mode.MONO) == Mode.MONO;
    }

    @Override
    public Authentication restoreAuthentication(HttpServletRequest request) {
        val argName = properties.getArgName();
        val headerName = properties.getTokenName();
        val cookieConfig = properties.getCookie();
        val cookieName = cookieConfig.getCookieName();
        val bearer = properties.getBearer();

        String accessToken = null;
        if (StringUtils.isNotBlank(argName)) {
            accessToken = StringUtils.defaultIfBlank(request.getParameter(argName), null);
            if (log.isTraceEnabled()) {
                log.trace("Try to get token from parameter({}): {}", argName, accessToken);
            }
        }

        val token = request.getHeader(headerName);
        if (StringUtils.isBlank(accessToken) && StringUtils.isNotBlank(token)) {
            if (StringUtils.isNotBlank(bearer) && token.startsWith(bearer)) {
                accessToken = token.substring(bearer.length() + 1);
                if (log.isTraceEnabled()) {
                    log.trace("Try to get token from header({}: {}): {}", headerName, bearer, accessToken);
                }
            }
        }

        if (StringUtils.isBlank(accessToken) && StringUtils.isNotBlank(cookieName)) {
            accessToken = GsvcContextHolder.cookies().get(cookieName).getValue();
            if (log.isTraceEnabled()) {
                log.trace("Try to get token from cookie({}): {}", cookieName, accessToken);
            }
        }

        if (StringUtils.isNotBlank(accessToken)) {
            val authentication = restoreAuthentication(accessToken);
            if (authentication instanceof AbstractAuthenticationToken jwtAuthenticationToken) {
                jwtAuthenticationToken.setDetails(DeviceAuthenticationDetails.create());
            }
            return authentication;
        }
        return null;
    }

    @Override
    public Authentication restoreAuthentication(String accessToken) {
        boolean verified;
        try {
            verified = JWTUtil.verify(accessToken, jwtSigner);
        }
        catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("accessToken is invalid: {}", accessToken, e);
            }
            throw TokenException.INVALID_TOKEN;
        }

        if (verified) {
            val jwt = JWTUtil.parseToken(accessToken);
            jwt.setSigner(jwtSigner);
            val jwtLeeway = properties.getJwtLeeway();
            if (!jwt.validate(jwtLeeway.toSeconds())) {
                log.trace("accessToken is expired: {}", accessToken);
                throw TokenException.EXPIRED;
            }

            val jwtToken = SimpleJwtToken.builder()
                .accessToken(accessToken)
                .name((String) jwt.getPayload(JWT.SUBJECT))
                .build();

            if (jwt.getPayload(PAYLOAD_UID) != null) {
                jwtToken.setUid((String) jwt.getPayload(PAYLOAD_UID));
            }
            if (jwt.getPayload(PAYLOAD_PD) != null) {
                jwtToken.setProvider((String) jwt.getPayload(PAYLOAD_PD));
            }
            if (jwt.getPayload(PAYLOAD_RUNAS) != null) {
                jwtToken.setRunAs((String) jwt.getPayload(PAYLOAD_RUNAS));
            }

            final String detail;
            if (validateAccessToken || jwt.getPayload(PAYLOAD_DETAIL) == null) {
                detail = null;
            }
            else {
                detail = (String) jwt.getPayload(PAYLOAD_DETAIL);
            }

            CachedUserDetails userDetails = null;

            if (detail != null) {
                try {
                    userDetails = objectMapper.readValue(detail, CachedUserDetails.class);
                    if (log.isTraceEnabled()) {
                        log.trace("Deserialized user details: {}", userDetails);
                    }
                }
                catch (JsonProcessingException e) {
                    log.warn("Failed to parse user details: {} - {}", detail, e.getMessage());
                    throw TokenException.INVALID_TOKEN;
                }
            }

            if (userDetails == null) {
                val username = jwtToken.getName();
                val tmpUser = User.withUsername(username).password("").build();
                userDetails = userDetailsMetaRepository.loadCachedUser(tmpUser);
            }

            if (userDetails == null) {
                log.trace("UserDetails of accessToken not found: {}", accessToken);
                throw new InvalidSessionException("Session is expired");
            }

            userDetails.setUsername(jwtToken.getName());

            // 使用空的authorities.
            val userDetailsMeta = userDetailsMetaRepository.create(userDetails);
            val authentication = JwtAuthenticationToken.authenticated(userDetailsMeta, "");

            if (detail == null && !Objects.equals(accessToken,
                    userDetailsMeta.cached(UserDetailsMeta.ACCESS_TOKEN_META_KEY, authentication))) {
                // refresh or login
                throw TokenException.INVALID_TOKEN;
            }
            if (detail == null) {
                // 创建一个微服务内部使用的TOKEN
                if (properties.isMfaEnabled()) {
                    userDetails.setMfaStatus(userDetailsMeta.get(UserDetailsMeta.MFA_STATUS_KEY, authentication));
                }
                jwtToken.setAccessToken(createAccessToken(userDetails, jwtToken));
            }
            else {
                jwtToken.setAccessToken(accessToken);
            }

            jwtToken.setMfa(userDetails.getMfaStatus());
            authentication.setJwtToken(jwtToken);

            log.trace("Authentication is restored from accessToken: {}", accessToken);
            return authentication;
        }
        else {
            log.trace("accessToken is invalid: {}", accessToken);
            throw TokenException.INVALID_TOKEN;
        }
    }

    @Override
    public JwtToken createJwtToken(@NonNull Authentication authentication) {
        if (authentication.getDetails() instanceof AuthenticationDetails device
                && !properties.deviceIsAllowed(device.getDevice())) {
            throw TokenException.DEVICE_NOT_ALLOWED;
        }

        val name = authentication.getName();
        JwtToken jwtToken = SimpleJwtToken.builder().name(name).build();
        if (authentication instanceof JwtAuthenticationToken token) {
            if (token.getJwtToken() != null) {
                jwtToken = token.getJwtToken();
            }
            if (token.getUserDetails().isPresent()) {
                val ud = token.getUserDetails().get();
                jwtToken.setLocked(!ud.isAccountNonLocked());
                jwtToken.setCredentialsExpired(!ud.isCredentialsNonExpired());
            }
        }

        val cs = customizers.orderedStream().toList();
        for (val c : cs) {
            jwtToken = c.customize(authentication, jwtToken);
        }

        jwtToken.setAccessToken(createAccessToken(null, jwtToken));
        jwtToken.setRefreshToken(createRefreshToken(jwtToken, authentication));

        return jwtToken;
    }

    @Override
    public JwtToken refreshAccessToken(@NonNull JwtToken jwtToken) {
        val authentication = refreshAuthentication(jwtToken);
        if (authentication != null) {
            return authentication.getJwtToken();
        }
        return null;
    }

    @Override
    public JwtAuthenticationToken refreshAuthentication(@NonNull JwtToken jwtToken) {
        try {
            val name = jwtToken.getName();
            if (StringUtils.isBlank(name)) {
                log.trace("refreshToken is invalid: {} - Username is blank", jwtToken.getRefreshToken());
                throw TokenException.INVALID_TOKEN;
            }

            val refreshToken = jwtToken.getRefreshToken();
            if (StringUtils.isBlank(refreshToken)) {
                log.trace("refreshToken is empty!");
                throw TokenException.INVALID_TOKEN;
            }

            try {
                JWTUtil.verify(refreshToken, jwtSigner);
            }
            catch (Exception e) {
                log.trace("refreshToken is invalid: {} - {}", refreshToken, e.getMessage());
                throw TokenException.INVALID_TOKEN;
            }

            val jwt = JWTUtil.parseToken(refreshToken);
            jwt.setSigner(jwtSigner);

            val jwtLeeway = properties.getJwtLeeway();
            if (!jwt.validate(jwtLeeway.toSeconds())) {
                log.trace("refreshToken is expired: {}", refreshToken);
                throw TokenException.EXPIRED;
            }

            val userDetails = userDetailsService.loadUserByUsername(name);

            UserDetailsMeta.checkUserDetails(userDetails);

            val accessToken = StringUtils.defaultString(jwtToken.getAccessToken());
            val password = userDetails.getPassword();
            val oldSign = (String) jwt.getPayload(JWT.SUBJECT);
            val sign = MD5.create().digestHex(accessToken + password);

            if (Objects.equals(oldSign, sign)) {
                val meta = userDetailsMetaRepository.create(userDetails);
                val authentication = JwtAuthenticationToken.unauthenticated(meta, userDetails.getPassword());
                val oldAccessKey = meta.get(UserDetailsMeta.ACCESS_TOKEN_META_KEY, authentication);
                if (!Objects.equals(accessToken, oldAccessKey)) {
                    throw new InvalidSessionException("invalid session");
                }
                if (jwt.getPayload(PAYLOAD_PD) != null) {
                    jwtToken.setProvider((String) jwt.getPayload(PAYLOAD_PD));
                }
                if (jwt.getPayload(PAYLOAD_UID) != null) {
                    jwtToken.setUid((String) jwt.getPayload(PAYLOAD_UID));
                }
                if (jwt.getPayload(PAYLOAD_RUNAS) != null) {
                    jwtToken.setRunAs((String) jwt.getPayload(PAYLOAD_RUNAS));
                }

                authentication.setJwtToken(jwtToken);

                if (!authentication.isLogin()) {
                    throw new InvalidSessionException("Not Login");
                }

                val newJwtToken = createJwtToken(authentication);

                authentication.login(newJwtToken);
                save(authentication, GsvcContextHolder.getRequest().orElse(null));
                return authentication;
            }

            log.trace("refreshToken is invalid: {} - accessToken or password does not match", refreshToken);
            throw TokenException.INVALID_TOKEN;
        }
        catch (Exception e) {
            log.trace("Cannot refresh accessToken: {}", e.getMessage());
        }

        return null;
    }

    @Override
    @NonNull
    public String createRefreshToken(@NonNull JwtToken jwtToken, @NonNull Authentication authentication) {
        val accessToken = jwtToken.getAccessToken();
        val principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            val password = userDetails.getPassword();
            val expire = properties.getRefreshTokenTimeout();
            val accessExpireAt = DateUtil.date().offset(DateField.MINUTE, (int) expire.toMinutes());
            val token = JWT.create();
            val refreshToken = MD5.create().digestHex(accessToken + password);
            token.setSubject(refreshToken);
            if (StringUtils.isNotBlank(jwtToken.getProvider())) {
                token.setPayload(PAYLOAD_PD, jwtToken.getProvider());
            }
            if (StringUtils.isNotBlank(jwtToken.getUid())) {
                token.setPayload(PAYLOAD_UID, jwtToken.getUid());
            }
            if (StringUtils.isNotBlank(jwtToken.getRunAs())) {
                token.setPayload(PAYLOAD_RUNAS, jwtToken.getRunAs());
            }
            token.setExpiresAt(accessExpireAt);
            token.setSigner(jwtSigner);
            return token.sign();
        }
        return "";
    }

    private String createAccessToken(UserDetails userDetails, @NonNull JwtToken jwtToken) {
        val token = JWT.create();
        if (StringUtils.isNotBlank(jwtToken.getUid())) {
            token.setPayload(PAYLOAD_UID, jwtToken.getUid());
        }
        if (StringUtils.isNotBlank(jwtToken.getProvider())) {
            token.setPayload(PAYLOAD_PD, jwtToken.getProvider());
        }
        if (StringUtils.isNotBlank(jwtToken.getRunAs())) {
            token.setPayload(PAYLOAD_RUNAS, jwtToken.getRunAs());
        }
        if (userDetails != null) {
            try {
                token.setPayload(PAYLOAD_DETAIL, objectMapper.writeValueAsString(userDetails));
            }
            catch (JsonProcessingException e) {
                log.warn("Cannot serialize user details: {}", userDetails);
            }
        }
        token.setSubject(jwtToken.getName());
        token.setSigner(jwtSigner);
        val accessExpireAt = DateUtil.date()
            .offset(DateField.MINUTE, (int) properties.getAccessTokenTimeout().toMinutes());
        token.setExpiresAt(accessExpireAt);

        return token.sign();
    }

}
