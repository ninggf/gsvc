/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.token;

import com.apzda.cloud.gsvc.security.authentication.AuthenticationDetails;
import com.apzda.cloud.gsvc.security.authentication.DeviceAuthenticationDetails;
import com.apzda.cloud.gsvc.security.userdetails.CachedUserDetails;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * @author fengz windywany@gmail.com
 **/
@Slf4j
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    protected JwtToken jwtToken;

    private final Object principal;

    private Object credentials;

    JwtAuthenticationToken(Object principal, Object credentials) {
        super(null);
        this.principal = checkPrincipal(principal);
        this.credentials = credentials;
        setAuthenticated(false);
        if (getDetails() == null) {
            setDetails(DeviceAuthenticationDetails.create());
        }
    }

    JwtAuthenticationToken(UserDetails principal, Object credentials,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = checkPrincipal(principal);
        this.credentials = credentials;
        super.setAuthenticated(true); // must use super, as we override

        if (getDetails() == null) {
            setDetails(DeviceAuthenticationDetails.create());
        }
    }

    public String deviceAwareMetaKey(String key) {
        val details = this.getDetails();
        if (details instanceof AuthenticationDetails device) {
            return key + "." + device.getApp() + "@" + device.getDevice();
        }
        return key;
    }

    public String deviceAwareMetaKey(String key, String subKey) {
        return deviceAwareMetaKey(key) + "." + subKey;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }

    public static JwtAuthenticationToken unauthenticated(Object principal, Object credentials) {
        return new JwtAuthenticationToken(principal, credentials);
    }

    public static JwtAuthenticationToken authenticated(UserDetails principal, Object credentials) {
        return new JwtAuthenticationToken(principal, credentials, Collections.emptyList());
    }

    public void setJwtToken(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
        if (jwtToken != null) {
            super.setAuthenticated(true);
            if (principal instanceof UserDetailsMeta meta) {
                meta.setUid(jwtToken.getUid());
                val provider = jwtToken.getProvider();
                if (StringUtils.isNotBlank(provider)) {
                    meta.setProvider(provider);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> getAuthorities() {
        log.trace("Retrieving authentication's authorities: {}!!", getName());
        if (principal instanceof UserDetails userDetails) {
            val authorities = userDetails.getAuthorities();
            if (!CollectionUtils.isEmpty(authorities)) {
                return (Collection<GrantedAuthority>) authorities;
            }
        }
        return Collections.emptyList();
    }

    public void logout() {
        if (jwtToken != null) {
            val accessToken = jwtToken.getAccessToken();
            val principal = getPrincipal();
            if (principal instanceof UserDetailsMeta userDetailsMeta) {
                try {
                    val key = UserDetailsMeta.ACCESS_TOKEN_META_KEY;
                    userDetailsMeta.remove(key, this);
                    this.setAuthenticated(false);

                    if (log.isTraceEnabled()) {
                        log.trace("accessToken({}) now is logout", accessToken);
                    }
                }
                catch (Exception e) {
                    if (log.isTraceEnabled()) {
                        log.trace("accessToken({}) logout failed: ", accessToken, e);
                    }
                }
            }
        }
    }

    public void login(@NonNull JwtToken jwtToken) {
        Assert.notNull(jwtToken, "JwtToken must not be null");
        setJwtToken(jwtToken);
        login();
    }

    public void login() {
        if (jwtToken != null && StringUtils.isNotBlank(jwtToken.getAccessToken())
                && principal instanceof UserDetailsMeta userDetailsMeta) {

            val key = UserDetailsMeta.ACCESS_TOKEN_META_KEY;
            userDetailsMeta.set(key, this, jwtToken.getAccessToken());

            userDetailsMeta.remove(UserDetailsMeta.AUTHORITY_META_KEY);

            val cachedUser = CachedUserDetails.from(userDetailsMeta);
            userDetailsMeta.set(UserDetailsMeta.CACHED_USER_DETAILS_KEY, cachedUser);

            val loginKey = UserDetailsMeta.LOGIN_TIME_META_KEY;
            if (userDetailsMeta.cached(loginKey, this, 0L) == 0) {
                userDetailsMeta.set(loginKey, this, System.currentTimeMillis());
            }
        }
    }

    public boolean isLogin() {
        if (!super.isAuthenticated() || jwtToken == null || StringUtils.isBlank(jwtToken.getAccessToken())) {
            return false;
        }

        if (principal instanceof UserDetailsMeta userDetailsMeta) {
            val accessToken = jwtToken.getAccessToken();
            val key = UserDetailsMeta.ACCESS_TOKEN_META_KEY;
            val cached = userDetailsMeta.cached(key, this);
            return Objects.equals(accessToken, cached);
        }
        return false;
    }

    public Optional<UserDetailsMeta> getUserDetails() {
        if (principal instanceof UserDetailsMeta userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    protected Object checkPrincipal(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            UserDetailsMeta.checkUserDetails(userDetails);
        }
        return principal;
    }

}
