package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

/**
 * @author fengz
 */
public interface UserDetailsMeta extends UserDetails {

    String AUTHORITY_META_KEY = "Authorities";

    String ACCESS_TOKEN_META_KEY = "AccessToken";

    String LOGIN_TIME_META_KEY = "LoginTime";

    String CACHED_USER_DETAILS_KEY = "CachedUserDetails";

    String MFA_STATUS_KEY = "MfaStatus";

    String LANGUAGE_KEY = "Language";

    @NonNull
    UserDetails getUserDetails();

    String getUid();

    void setUid(String uid);

    void setOpenId(String openId);

    String getOpenId();

    void setProvider(String provider);

    String getProvider();

    void setUnionId(String unionId);

    String getUnionId();

    void set(String key, Object value);

    default void set(String key, @NonNull Authentication authentication, Object value) {
        if (authentication instanceof JwtAuthenticationToken token) {
            set(token.deviceAwareMetaKey(key), value);
        }
        else {
            set(key, value);
        }
    }

    <R> Optional<R> get(String key, String metaKey, Class<R> rClass, boolean cached);

    @NonNull
    default <R> Optional<R> cached(String key, Class<R> rClass) {
        return get(key, key, rClass, true);
    }

    @NonNull
    default <R> Optional<R> get(String key, Class<R> rClass) {
        return get(key, key, rClass);
    }

    @NonNull
    default <R> Optional<R> get(String key, String metaKey, Class<R> rClass) {
        return get(key, metaKey, rClass, false);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    default <R> R cached(String key, @NonNull R defaultValue) {
        Optional<R> meta = (Optional<R>) cached(key, defaultValue.getClass());
        return meta.orElse(defaultValue);
    }

    @NonNull
    default <R> R cached(@NonNull String key, @NonNull Authentication authentication, @NonNull R defaultValue) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return cached(token.deviceAwareMetaKey(key), defaultValue);
        }
        return cached(key, defaultValue);
    }

    @Nullable
    default String cached(@NonNull String key, @NonNull Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return cached(token.deviceAwareMetaKey(key), String.class).orElse(null);
        }
        return cached(key, String.class).orElse(null);
    }

    @SuppressWarnings("unchecked")
    default <R> R get(String key, String metaKey, @NonNull R defaultValue) {
        Optional<R> meta = (Optional<R>) get(key, metaKey, defaultValue.getClass());
        return meta.orElse(defaultValue);
    }

    default <R> R get(String key, @NonNull R defaultValue) {
        return get(key, key, defaultValue);
    }

    @Nullable
    default String get(String key) {
        return get(key, key, String.class).orElse(null);
    }

    @Nullable
    default String get(String key, String metaKey) {
        return get(key, metaKey, String.class).orElse(null);
    }

    default <R> R get(@NonNull String key, @NonNull Authentication authentication, @NonNull R defaultValue) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return get(token.deviceAwareMetaKey(key), key, defaultValue);
        }
        return get(key, key, defaultValue);
    }

    @Nullable
    default String get(String key, @NonNull Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return get(token.deviceAwareMetaKey(key), key, String.class).orElse(null);
        }
        return get(key, key, String.class).orElse(null);
    }

    void remove(String key);

    default void remove(String key, @NonNull Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            remove(token.deviceAwareMetaKey(key));
        }
        else {
            remove(key);
        }
    }

    default void clear() {
    }

    static void checkUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UsernameNotFoundException("");
        }

        val username = userDetails.getUsername();
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("username is blank");
        }

        if (!userDetails.isEnabled()) {
            throw new DisabledException(String.format("%s is disabled", username));
        }

        if (!userDetails.isAccountNonExpired()) {
            throw new AccountExpiredException(String.format("%s's account is expired", username));
        }
    }

}
