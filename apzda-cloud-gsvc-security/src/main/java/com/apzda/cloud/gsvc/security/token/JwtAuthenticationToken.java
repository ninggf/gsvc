/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.token;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.security.authentication.DeviceAuthenticationDetails;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * @author fengz windywany@gmail.com
 **/
@Slf4j
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    protected JwtToken jwtToken;

    private final Object principal;

    private Object credentials;

    public JwtAuthenticationToken(Object principal, Object credentials) {
        super(null);
        this.principal = checkPrincipal(principal);
        this.credentials = credentials;
        setAuthenticated(false);
        if (getDetails() == null) {
            setDetails(DeviceAuthenticationDetails.create());
        }
    }

    public JwtAuthenticationToken(UserDetails principal, Object credentials,
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
        if (details instanceof DeviceAuthenticationDetails deviceAuthenticationDetails) {
            return key + "." + deviceAuthenticationDetails.getDevice();
        }
        return key;
    }

    public String deviceAwareMetaKey(String key, String subKey) {
        val details = this.getDetails();
        if (details instanceof DeviceAuthenticationDetails deviceAuthenticationDetails) {
            key = key + "." + deviceAuthenticationDetails.getDevice();
        }
        return key + "." + subKey;
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

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> getAuthorities() {
        if (this.getPrincipal() instanceof UserDetails userDetails) {
            val authorities = userDetails.getAuthorities();
            if (!CollectionUtils.isEmpty(authorities)) {
                return (Collection<GrantedAuthority>) authorities;
            }
        }
        return Collections.emptyList();
    }

    public JwtToken getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }

    public void logout() {
        if (jwtToken != null) {
            val accessToken = jwtToken.getAccessToken();
            val principal = getPrincipal();
            if (principal instanceof UserDetailsMeta userDetailsMeta) {

                try {
                    val key = deviceAwareMetaKey(UserDetailsMeta.ACCESS_TOKEN_META_KEY,
                            UserDetailsMeta.LOGIN_TIME_SUB_KEY);

                    userDetailsMeta.remove(key);

                    if (log.isTraceEnabled()) {
                        log.trace("[{}] accessToken({}) now is logout", GsvcContextHolder.getRequestId(), accessToken);
                    }
                }
                catch (Exception e) {
                    if (log.isTraceEnabled()) {
                        log.trace("[{}] accessToken({}) logout failed: ", GsvcContextHolder.getRequestId(), accessToken,
                                e);
                    }
                }
            }
        }
    }

    public void login(@NonNull JwtToken jwtToken) {
        Assert.notNull(jwtToken, "JwtToken must not be null");
        this.jwtToken = jwtToken;
        login();
    }

    public void login() {
        if (principal instanceof UserDetailsMeta userDetailsMeta) {
            val key = deviceAwareMetaKey(UserDetailsMeta.ACCESS_TOKEN_META_KEY, UserDetailsMeta.LOGIN_TIME_SUB_KEY);
            userDetailsMeta.set(key, System.currentTimeMillis());
        }
    }

    public boolean isLogin() {
        if (principal instanceof UserDetailsMeta userDetailsMeta) {
            val key = deviceAwareMetaKey(UserDetailsMeta.ACCESS_TOKEN_META_KEY, UserDetailsMeta.LOGIN_TIME_SUB_KEY);
            return userDetailsMeta.get(key, Long.valueOf("0")) > 0;
        }
        return false;
    }

    protected Object checkPrincipal(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            UserDetailsMeta.checkUserDetails(userDetails);
        }
        return principal;
    }

}
