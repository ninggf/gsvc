package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.val;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author fengz
 */
public class InMemoryUserDetailsContainer extends AbstractUserDetailsContainer {

    private static LoadingCache<String, UserMeta> USER_DETAILS_META;

    public static void init(SecurityConfigProperties properties) {
        USER_DETAILS_META = CacheBuilder.newBuilder().build(new CacheLoader<String, UserMeta>() {
            @Override
            public UserMeta load(String key) throws Exception {
                return new UserMeta();
            }
        });
    }

    public InMemoryUserDetailsContainer(UserDetails userDetails, UserDetailsService userDetailsService) {
        super(userDetails, userDetailsService);
    }

    @Override
    public void setMeta(String key, Object value) throws ExecutionException {
        val userMeta = USER_DETAILS_META.get(userDetails.getUsername());
        userMeta.put(key, value);
    }

    @Override
    public <R> Optional<R> getMeta(String key, Class<R> rClass) {
        try {
            val userMeta = USER_DETAILS_META.get(userDetails.getUsername());
            val meta = userMeta.get(key);
            if (meta != null) {
                return Optional.of(rClass.cast(meta));
            }
        }
        catch (Exception ignored) {
        }
        return Optional.empty();
    }

    @Override
    public void removeMeta(String key) {
        val meta = USER_DETAILS_META.getIfPresent(userDetails.getUsername());
        if (meta != null) {
            meta.remove(key);
        }
    }

    @Override
    public void invalidate() {
        USER_DETAILS_META.invalidate(userDetails.getUsername());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> getAuthorities() {
        val authorityMeta = getMeta("Authorities", Collection.class);
        if (authorityMeta.isPresent()) {
            return (Collection<GrantedAuthority>) authorityMeta.get();
        }

        val ud = userDetailsService.loadUserByUsername(userDetails.getUsername());
        val authorities = ud.getAuthorities();
        try {
            setMeta("Authorities", authorities);
        }
        catch (ExecutionException e) {
            return Collections.emptyList();
        }

        return authorities;
    }

    static class UserMeta extends ConcurrentHashMap<String, Object> {

    }

}
