package com.apzda.cloud.gsvc.security.userdetails;

import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * @author fengz
 */
public class DefaultUserDetailsMeta implements UserDetailsMeta {

    protected final UserDetails userDetails;

    protected final UserDetailsMetaRepository userDetailsMetaRepository;

    protected Collection<? extends GrantedAuthority> authorities;

    public DefaultUserDetailsMeta(@NonNull UserDetails userDetails,
                                  @NonNull UserDetailsMetaRepository userDetailsMetaRepository) {
        this.userDetails = userDetails;
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
    public <R> Optional<R> get(String key, Class<R> rClass) {
        return this.userDetailsMetaRepository.getMetaData(this.userDetails, key, rClass);
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
            return this.authorities;
        }
        this.authorities = this.userDetailsMetaRepository.getAuthorities(this.userDetails);
        if (this.authorities == null) {
            authorities = Collections.emptyList();
        }
        return this.authorities;
    }

}
