package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author fengz
 */
@Slf4j
public class InMemoryUserServiceMetaRepository implements UserDetailsMetaRepository {

    private final UserDetailsService userDetailsService;

    private final LoadingCache<String, UserMeta> userDetailsMetaCache;

    public InMemoryUserServiceMetaRepository(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        this.userDetailsMetaCache = CacheBuilder.newBuilder().build(new UserMetaLoader());
    }

    @Override
    @NonNull
    public UserDetailsMeta create(@NonNull UserDetails userDetails) {
        return new DefaultUserDetailsMeta(userDetails, this);
    }

    @Override
    public void setMetaData(UserDetails userDetails, String key, Object value) throws ExecutionException {
        val userMeta = userDetailsMetaCache.get(userDetails.getUsername());
        userMeta.put(key, value);
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
            if (log.isTraceEnabled()) {
                log.trace("[{}] Cannot load user meta for {} - {}", GsvcContextHolder.getRequestId(),
                        userDetails.getUsername(), e.getMessage());
            }
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

    @Override
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> getAuthorities(UserDetails userDetails) {
        val authorityMeta = getMetaData(userDetails, UserDetailsMeta.AUTHORITY_META_KEY, Collection.class);
        if (authorityMeta.isPresent()) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Load user's authorities from cache: {}", GsvcContextHolder.getRequestId(),
                        userDetails.getUsername());
            }
            return (Collection<? extends GrantedAuthority>) authorityMeta.get();
        }

        try {
            val ud = userDetailsService.loadUserByUsername(userDetails.getUsername());
            var authorities = ud.getAuthorities();
            if (CollectionUtils.isEmpty(authorities)) {
                authorities = Collections.emptyList();
            }
            setMetaData(userDetails, UserDetailsMeta.AUTHORITY_META_KEY, authorities);
            if (log.isTraceEnabled()) {
                log.trace("[{}] Load user's authorities from userDetailsService: {}", GsvcContextHolder.getRequestId(),
                        userDetails.getUsername());
            }
            return authorities;
        }
        catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] cannot load user's authorities: {} - {}", GsvcContextHolder.getRequestId(),
                        userDetails.getUsername(), e.getMessage());
            }
        }
        return Collections.emptyList();
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
