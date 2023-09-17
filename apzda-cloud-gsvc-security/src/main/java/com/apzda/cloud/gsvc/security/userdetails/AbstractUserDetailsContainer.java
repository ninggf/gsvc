package com.apzda.cloud.gsvc.security.userdetails;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author fengz
 */
public abstract class AbstractUserDetailsContainer implements UserDetailsContainer {

    protected final UserDetails userDetails;

    protected final UserDetailsService userDetailsService;

    private final int flags;

    public AbstractUserDetailsContainer(@NonNull UserDetails userDetails, UserDetailsService userDetailsService) {
        this.userDetails = userDetails;
        this.userDetailsService = userDetailsService;
        int flags = 0;
        if (userDetails.isAccountNonExpired()) {
            flags = flags | UserFlag.isAccountNonExpired.flag;
        }
        if (userDetails.isAccountNonLocked()) {
            flags = flags | UserFlag.isAccountNonLocked.flag;
        }
        if (userDetails.isCredentialsNonExpired()) {
            flags = flags | UserFlag.isCredentialsNonExpired.flag;
        }
        if (userDetails.isEnabled()) {
            flags = flags | UserFlag.isEnabled.flag;
        }
        this.flags = flags;
    }

    @Override
    @NonNull
    public UserDetails get() {
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
    public int flags() {
        return this.flags;
    }

}
