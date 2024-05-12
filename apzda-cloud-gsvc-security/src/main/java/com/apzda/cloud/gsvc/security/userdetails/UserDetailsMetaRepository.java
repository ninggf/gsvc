package com.apzda.cloud.gsvc.security.userdetails;

import cn.hutool.core.lang.ParameterizedTypeImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

/**
 * @author fengz
 */
public interface UserDetailsMetaRepository {

    @NonNull
    UserDetailsMeta create(@NonNull UserDetails userDetails);

    void setMetaData(UserDetails userDetails, String key, Object value);

    @NonNull
    <R> Optional<R> getMetaData(UserDetails userDetails, String key, String metaKey, Class<R> rClass);

    @NonNull
    default <R> Optional<R> getMetaData(UserDetails userDetails, String key, Class<R> rClass) {
        return getMetaData(userDetails, key, key, rClass);
    }

    @NonNull
    default <R> Optional<Collection<R>> getMultiMetaData(UserDetails userDetails, String key, String metaKey,
            Class<R> rClass) {
        val typeReference = new TypeReference<Collection<R>>() {
            @Override
            public Type getType() {
                return new ParameterizedTypeImpl(new Type[] { rClass }, null, Collection.class);
            }
        };

        return getMultiMetaData(userDetails, key, metaKey, typeReference);
    }

    @NonNull
    <R> Optional<R> getMultiMetaData(UserDetails userDetails, String key, String metaKey,
            TypeReference<R> typeReference);

    @SuppressWarnings("unchecked")
    default <R> R getMetaData(UserDetails userDetails, String key, String metaKey, @NonNull R defaultValue) {
        Optional<R> meta = (Optional<R>) getMetaData(userDetails, key, metaKey, defaultValue.getClass());
        return meta.orElse(defaultValue);
    }

    default String getMetaData(UserDetails userDetails, String key) {
        return getMetaData(userDetails, key, key, "");
    }

    default String getString(UserDetails userDetails, String key, String metaKey) {
        return getMetaData(userDetails, key, metaKey, "");
    }

    void removeMetaData(UserDetails userDetails, String key);

    void removeMetaData(UserDetails userDetails);

    Collection<? extends GrantedAuthority> getAuthorities(UserDetails userDetails);

    @NonNull
    <R> Optional<R> getCachedMetaData(UserDetails userDetails, String key, String metaKey,
            TypeReference<R> typeReference);

    @NonNull
    <R> Optional<R> getCachedMetaData(UserDetails userDetails, String key, String metaKey, Class<R> rClass);

}
