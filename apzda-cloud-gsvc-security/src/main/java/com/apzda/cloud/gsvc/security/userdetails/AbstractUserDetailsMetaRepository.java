package com.apzda.cloud.gsvc.security.userdetails;

import cn.hutool.core.lang.ParameterizedTypeImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * @author fengz
 */
@Slf4j
public abstract class AbstractUserDetailsMetaRepository implements UserDetailsMetaRepository {

    protected final UserDetailsMetaService userDetailsMetaService;

    protected final Class<? extends GrantedAuthority> authorityClass;

    protected final TypeReference<Collection<? extends GrantedAuthority>> typeReference;

    protected AbstractUserDetailsMetaRepository(UserDetailsMetaService userDetailsMetaService,
            Class<? extends GrantedAuthority> authorityClass) {
        this.userDetailsMetaService = userDetailsMetaService;
        this.authorityClass = authorityClass;
        this.typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return new ParameterizedTypeImpl(new Type[] { authorityClass }, null, Collection.class);
            }
        };
    }

    @Override
    @NonNull
    public UserDetailsMeta create(@NonNull UserDetails userDetails) {
        return new DefaultUserDetailsMeta(userDetails, this);
    }

    @Override
    @NonNull
    public <R> Optional<R> getMetaData(UserDetails userDetails, String key, String metaKey, Class<R> rClass) {
        val cached = getCachedMetaData(userDetails, key, metaKey, rClass);
        if (cached.isPresent()) {
            return cached;
        }
        val metaData = userDetailsMetaService.getMetaData(userDetails, metaKey, rClass);
        metaData.ifPresent(r -> setMetaData(userDetails, key, r));
        return metaData;
    }

    @Override
    @NonNull
    public <R> Optional<R> getMultiMetaData(UserDetails userDetails, String key, String metaKey,
            TypeReference<R> typeReference) {
        val meta = getCachedMetaData(userDetails, key, metaKey, typeReference);
        if (meta.isPresent()) {
            return meta;
        }
        val metaData = userDetailsMetaService.getMultiMetaData(userDetails, metaKey, typeReference);
        metaData.ifPresent(r -> setMetaData(userDetails, key, r));
        return metaData;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(UserDetails userDetails) {
        val authorityMeta = getCachedMetaData(userDetails, UserDetailsMeta.AUTHORITY_META_KEY,
                UserDetailsMeta.AUTHORITY_META_KEY, typeReference);

        if (authorityMeta.isPresent()) {
            if (log.isTraceEnabled()) {
                log.trace("User's Authorities loaded from meta repository: {}", userDetails.getUsername());
            }
            return authorityMeta.get();
        }

        try {
            var authorities = userDetailsMetaService.getAuthorities(userDetails);
            if (CollectionUtils.isEmpty(authorities)) {
                authorities = Collections.emptyList();
            }
            setMetaData(userDetails, UserDetailsMeta.AUTHORITY_META_KEY, authorities);
            if (log.isTraceEnabled()) {
                log.trace("User's Authorities loaded by userDetailsMetaService: {}", userDetails.getUsername());
            }
            return authorities;
        }
        catch (Exception e) {
            log.warn("Cannot load user's authorities: {} - {}", userDetails.getUsername(), e.getMessage());
        }
        return Collections.emptyList();
    }

}
