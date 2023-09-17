package com.apzda.cloud.gsvc.security.userdetails;

import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author fengz
 */
public interface UserDetailsMetaRepository {

    @NonNull
    UserDetailsMeta create(@NonNull UserDetails userDetails);

    void setMetaData(UserDetails userDetails, String key, Object value) throws ExecutionException;

    <R> Optional<R> getMetaData(UserDetails userDetails, String key, Class<R> rClass);

    @SuppressWarnings("unchecked")
    default <R> R getMetaData(UserDetails userDetails, String key, @NonNull R defaultValue) {
        Optional<R> meta = (Optional<R>) getMetaData(userDetails, key, defaultValue.getClass());
        return meta.orElse(defaultValue);
    }

    default String getString(UserDetails userDetails, String key) {
        return getMetaData(userDetails, key, "");
    }

    void removeMetaData(UserDetails userDetails, String key);

    void removeMetaData(UserDetails userDetails);

    Collection<? extends GrantedAuthority> getAuthorities(UserDetails userDetails);

}
