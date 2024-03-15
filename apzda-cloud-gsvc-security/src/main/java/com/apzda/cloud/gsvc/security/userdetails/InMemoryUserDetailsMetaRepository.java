package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
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
        } catch (ExecutionException e) {
            log.error("[{}] Cannot set user meta: {}.{} = {}", GsvcContextHolder.getRequestId(),
                userDetails.getUsername(), key, value, e);
        }
    }

    @Override
    @NonNull
    public <R> Optional<R> getMetaData(UserDetails userDetails, String key, Class<R> rClass) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                if (log.isTraceEnabled()) {
                    log.trace("[{}] User meta '{}' of '{}' loaded from Memory", GsvcContextHolder.getRequestId(), key, userDetails.getUsername());
                }
                return Optional.of(rClass.cast(meta));
            }
            val metaData = userDetailsMetaService.getMetaData(userDetails, key, rClass);
            metaData.ifPresent(r -> setMetaData(userDetails, key, r));
            return metaData;
        } catch (Exception e) {
            log.error("[{}] Cannot load user meta for {}.{} - {}", GsvcContextHolder.getRequestId(),
                userDetails.getUsername(), key, e.getMessage());

        }
        return Optional.empty();
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public <R> Optional<R> getMultiMetaData(UserDetails userDetails, String key, TypeReference<R> typeReference) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                if (log.isTraceEnabled()) {
                    log.trace("[{}] User metas '{}' of '{}' loaded from Memory", GsvcContextHolder.getRequestId(), key, userDetails.getUsername());
                }
                return Optional.of((R) meta);
            }
            val metaData = userDetailsMetaService.getMultiMetaData(userDetails, key, typeReference);
            metaData.ifPresent(r -> setMetaData(userDetails, key, r));
            return metaData;
        } catch (Exception e) {
            log.error("[{}] Cannot load user meta for {}.{} - {}", GsvcContextHolder.getRequestId(),
                userDetails.getUsername(), key, e.getMessage());

        }
        return Optional.empty();
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
