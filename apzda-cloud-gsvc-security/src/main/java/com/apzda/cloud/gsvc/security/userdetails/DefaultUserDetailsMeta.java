package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fengz
 */
@Slf4j
public class DefaultUserDetailsMeta implements UserDetailsMeta {

    protected final UserDetails userDetails;

    protected final UserDetailsMetaRepository userDetailsMetaRepository;

    protected final Map<String, Object> metas = new ConcurrentHashMap<>();

    protected Collection<? extends GrantedAuthority> authorities;

    protected String uid;

    protected String openId;

    protected String unionId;

    protected String provider;

    protected String tenantId;

    protected Authentication authentication;

    public DefaultUserDetailsMeta(@NonNull UserDetails userDetails,
            @NonNull UserDetailsMetaRepository userDetailsMetaRepository) {
        if (userDetails instanceof UserDetailsMeta userDetailsMeta) {
            this.userDetails = userDetailsMeta.getUserDetails();
        }
        else {
            this.userDetails = userDetails;
        }
        Assert.notNull(this.userDetails, "userDetails must not be null");

        this.userDetailsMetaRepository = userDetailsMetaRepository;
        this.openId = this.userDetails.getUsername();
        this.unionId = this.openId;
        this.provider = "db";
    }

    @Override
    @NonNull
    public UserDetails getUserDetails() {
        return this.userDetails;
    }

    @Override
    public Authentication getAuthentication() {
        return this.authentication;
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public String getPassword() {
        return this.userDetails.getPassword();
    }

    @Override
    public String getUsername() {
        return this.userDetails.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.userDetails.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.userDetails.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.userDetails.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return this.userDetails.isEnabled();
    }

    @Override
    public void set(String key, Object value) {
        this.userDetailsMetaRepository.setMetaData(this.userDetails, key, value);
    }

    @Override
    public <R> Optional<R> get(String key, String metaKey, Class<R> rClass, boolean cached) {
        if (metas.containsKey(key)) {
            return Optional.of(rClass.cast(metas.get(key)));
        }
        if (cached) {
            val cachedMeta = this.userDetailsMetaRepository.getCachedMetaData(this.userDetails, key, metaKey, rClass);
            cachedMeta.ifPresent(r -> metas.put(key, r));
            return cachedMeta;
        }
        else {
            val meta = this.userDetailsMetaRepository.getMetaData(this.userDetails, key, metaKey, rClass);
            meta.ifPresent(r -> metas.put(key, r));
            return meta;
        }
    }

    @Override
    public void remove(String key) {
        this.userDetailsMetaRepository.removeMetaData(this.userDetails, key);
    }

    @Override
    public void clear() {
        this.userDetailsMetaRepository.removeMetaData(this.userDetails);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.authorities != null) {
            if (log.isTraceEnabled()) {
                log.trace("Load authorities from cache: {}", this.getUsername());
            }
            return this.authorities;
        }
        if (log.isTraceEnabled()) {
            log.trace("Load authorities from userDetailsMetaRepository: {}", this.getUsername());
        }
        this.authorities = this.userDetailsMetaRepository.getAuthorities(this.userDetails);
        if (this.authorities == null) {
            authorities = Collections.emptyList();
        }
        return this.authorities;
    }

    @Override
    public void setOpenId(String openId) {
        this.openId = openId;
    }

    @Override
    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String getOpenId() {
        return this.openId;
    }

    @Override
    public String getTenantId() {
        if (!StringUtils.hasText(tenantId)) {
            if (this.authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                tenantId = this.userDetailsMetaRepository.getTenantId(this.userDetails,
                        jwtAuthenticationToken.deviceAwareMetaKey(UserDetailsMeta.CURRENT_TENANT_ID));
            }
            else {
                tenantId = this.userDetailsMetaRepository.getTenantId(this.userDetails,
                        UserDetailsMeta.CURRENT_TENANT_ID);
            }
        }
        return tenantId;
    }

    @Override
    public String getProvider() {
        return this.provider;
    }

    @Override
    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    @Override
    public String getUnionId() {
        return this.unionId;
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public void setUid(String uid) {
        this.uid = uid;
    }

}
