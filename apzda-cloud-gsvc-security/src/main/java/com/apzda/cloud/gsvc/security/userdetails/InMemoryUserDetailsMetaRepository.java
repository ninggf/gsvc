package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author fengz
 */
@Slf4j
public class InMemoryUserDetailsMetaRepository extends AbstractUserDetailsMetaRepository {

    private final LoadingCache<String, UserMeta> userDetailsMetaCache;

    public InMemoryUserDetailsMetaRepository(UserDetailsService userDetailsService,
            Class<? extends GrantedAuthority> authorityClass) {
        super(userDetailsService, authorityClass);
        this.userDetailsMetaCache = CacheBuilder.newBuilder().build(new UserMetaLoader());
    }

    @Override
    public void setMetaData(UserDetails userDetails, String key, Object value) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            userMeta.put(key, value);
        }
        catch (ExecutionException e) {
            log.error("[{}] Cannot set user meta: {}.{} = {}", GsvcContextHolder.getRequestId(),
                    userDetails.getUsername(), key, value, e);
        }
    }

    @Override
    public <R> Optional<R> getMetaData(UserDetails userDetails, String key, Class<R> rClass) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                return Optional.of(rClass.cast(meta));
            }
        }
        catch (Exception e) {
            log.error("[{}] Cannot load user meta for {}.{} - {}", GsvcContextHolder.getRequestId(),
                    userDetails.getUsername(), key, e.getMessage());

        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Optional<Collection<R>> getMetaDataByHint(UserDetails userDetails, String key, Class<R> rClass) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                return Optional.of((Collection<R>) meta);
            }
        }
        catch (Exception e) {
            log.error("[{}] Cannot load user meta for {}.{} - {}", GsvcContextHolder.getRequestId(),
                    userDetails.getUsername(), key, e.getMessage());

        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Optional<R> getMetaDataByHint(UserDetails userDetails, String key, TypeReference<R> typeReference) {
        try {
            val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                return Optional.of((R) meta);
            }
        }
        catch (Exception e) {
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
        public UserMeta load(String key) throws Exception {
            return new UserMeta();
        }

    }

}
