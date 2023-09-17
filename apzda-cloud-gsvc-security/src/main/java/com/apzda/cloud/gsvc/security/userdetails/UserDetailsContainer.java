package com.apzda.cloud.gsvc.security.userdetails;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author fengz
 */
public interface UserDetailsContainer extends UserDetails {

    @NonNull
    UserDetails get();

    void setMeta(String key, Object value) throws ExecutionException;

    <R> Optional<R> getMeta(String key, Class<R> rClass);

    void removeMeta(String key);

    void invalidate();

    int flags();

}
