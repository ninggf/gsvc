package com.apzda.cloud.gsvc.security.userdetails;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author fengz
 */
@RequiredArgsConstructor
public class InMemoryUserServiceWrapper implements UserDetailsWrapper {

    private final UserDetailsService userDetailsService;

    @Override
    @NonNull
    public UserDetailsContainer wrap(@NonNull UserDetails userDetails) {
        return new InMemoryUserDetailsContainer(userDetails, userDetailsService);
    }

}
