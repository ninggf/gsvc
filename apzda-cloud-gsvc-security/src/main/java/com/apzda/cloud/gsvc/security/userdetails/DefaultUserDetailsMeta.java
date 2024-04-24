package com.apzda.cloud.gsvc.security.userdetails;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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

    public DefaultUserDetailsMeta(@NonNull UserDetails userDetails,
            @NonNull UserDetailsMetaRepository userDetailsMetaRepository) {
        if (userDetails instanceof UserDetailsMeta userDetailsMeta) {
            this.userDetails = userDetailsMeta.getUserDetails();
        }
        else {
            this.userDetails = userDetails;
        }

        this.userDetailsMetaRepository = userDetailsMetaRepository;
    }

    @Override
    @NonNull
    public UserDetails getUserDetails() {
        return this.userDetails;
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
    public <R> Optional<R> get(String key, String metaKey, Class<R> rClass) {
        if (metas.containsKey(key)) {
            return Optional.of(rClass.cast(metas.get(key)));
        }
        val meta = this.userDetailsMetaRepository.getMetaData(this.userDetails, key, metaKey, rClass);
        meta.ifPresent(r -> metas.put(key, r));
        return meta;
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

}
