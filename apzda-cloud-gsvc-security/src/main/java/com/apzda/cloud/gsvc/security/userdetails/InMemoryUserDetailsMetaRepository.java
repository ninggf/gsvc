package com.apzda.cloud.gsvc.security.userdetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author fengz
 */
@Slf4j
public class InMemoryUserDetailsMetaRepository extends AbstractUserDetailsMetaRepository {

    private final LoadingCache<String, UserMeta> userDetailsMetaCache;

    public InMemoryUserDetailsMetaRepository(UserDetailsMetaService userDetailsMetaService,
            Class<? extends GrantedAuthority> authorityClass) {
        super(userDetailsMetaService, authorityClass);
        this.userDetailsMetaCache = CacheBuilder.newBuilder().build(new UserMetaLoader());
    }

    @Override
    public void setMetaData(UserDetails userDetails, String key, Object value) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            userMeta.put(key, value);
        }
        catch (ExecutionException e) {
            log.error("Cannot set user meta: {}.{} = {}", userDetails.getUsername(), key, value, e);
        }
    }

    @Override
    public void removeMetaData(UserDetails userDetails, String key) {
        val meta = userDetailsMetaCache.getIfPresent(userDetails.getUsername());
        if (meta != null) {
            meta.remove(key);
            log.trace("User meta '{}' of '{}' removed", key, userDetails.getUsername());
        }
    }

    @Override
    public void removeMetaData(UserDetails userDetails) {
        userDetailsMetaCache.invalidate(userDetails.getUsername());
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public <R> Optional<R> getCachedMetaData(UserDetails userDetails, String key, String metaKey,
            TypeReference<R> typeReference) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                if (log.isTraceEnabled()) {
                    log.trace("User metas '{}' of '{}' loaded from Memory", key, userDetails.getUsername());
                }
                return Optional.of((R) meta);
            }
        }
        catch (Exception e) {
            log.error("Cannot load user metas for {}.{} - {}", userDetails.getUsername(), key, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    @NonNull
    public <R> Optional<R> getCachedMetaData(UserDetails userDetails, String key, String metaKey, Class<R> rClass) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                if (log.isTraceEnabled()) {
                    log.trace("User meta '{}' of '{}' loaded from Memory", key, userDetails.getUsername());
                }
                return Optional.of(rClass.cast(meta));
            }
        }
        catch (Exception e) {
            log.error("Cannot load user meta for {}.{} - {}", userDetails.getUsername(), key, e.getMessage());
        }
        return Optional.empty();
    }

    static class UserMeta extends ConcurrentHashMap<String, Object> {

    }

    static class UserMetaLoader extends CacheLoader<String, UserMeta> {

        @Override
        @NonNull
        public UserMeta load(@NonNull String key) throws Exception {
            return new UserMeta();
        }

    }

}
