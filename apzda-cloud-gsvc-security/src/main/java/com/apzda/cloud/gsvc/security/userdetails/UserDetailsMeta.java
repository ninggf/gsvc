package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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

    default String getOpenId() {
        return getUserDetails().getUsername();
    }

    default String getProvider() {
        return "db";
    }

    void set(String key, Object value);

    default void set(String key, @NonNull Authentication authentication, Object value) {
        if (authentication instanceof JwtAuthenticationToken token) {
            set(token.deviceAwareMetaKey(key), value);
        }
        set(key, value);
    }

    <R> Optional<R> get(String key, Class<R> rClass);

    default String getString(String key) {
        return get(key, "");
    }

    @SuppressWarnings("unchecked")
    default <R> R get(String key, @NonNull R defaultValue) {
        Optional<R> meta = (Optional<R>) get(key, defaultValue.getClass());
        return meta.orElse(defaultValue);
    }

    default <R> R get(@NonNull String key, @NonNull Authentication authentication, @NonNull R defaultValue) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return get(token.deviceAwareMetaKey(key), defaultValue);
        }
        return get(key, defaultValue);
    }

    void remove(String key);

    default void remove(String key, @NonNull Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            remove(token.deviceAwareMetaKey(key));
        }
        remove(key);
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
        if (!userDetails.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(String.format("%s's password is expired", username));
        }
        if (!userDetails.isAccountNonExpired()) {
            throw new AccountExpiredException(String.format("%s's account is expired", username));
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new LockedException(String.format("%s's account is expired", username));
        }
    }

}
