package com.apzda.cloud.gsvc.security.userdetails;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author fengz
 */
public interface UserDetailsWrapper {

    @NonNull
    UserDetailsContainer wrap(@NonNull UserDetails userDetails);

}
