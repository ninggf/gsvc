package com.apzda.cloud.gsvc.security.userdetails;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * @author fengz
 */
public interface UserDetailsMeta extends UserDetails {

    static String AUTHORITY_META_KEY = "Authorities";
    static String ACCESS_TOKEN_META_KEY = "AccessToken";
    static String REFRESH_TOKEN_META_KEY = "RefreshToken";

    static String LOGIN_TIME_SUB_KEY = "loginTime";

    @NonNull
    UserDetails getUserDetails();

    void set(String key, Object value);

    <R> Optional<R> get(String key, Class<R> rClass);

    default String getString(String key) {
        return get(key, "");
    }

    @SuppressWarnings("unchecked")
    default <R> R get(String key, @NonNull R defaultValue) {
        Optional<R> meta = (Optional<R>) get(key, defaultValue.getClass());
        return meta.orElse(defaultValue);
    }

    void remove(String key);

    void clear();

}
