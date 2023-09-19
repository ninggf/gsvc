package com.apzda.cloud.gsvc.security.userdetails;

import cn.hutool.core.lang.ParameterizedTypeImpl;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

/**
 * @author fengz
 */
@Slf4j
public abstract class AbstractUserDetailsMetaRepository implements UserDetailsMetaRepository {

    protected final UserDetailsService userDetailsService;

    protected final Class<? extends GrantedAuthority> authorityClass;

    protected AbstractUserDetailsMetaRepository(UserDetailsService userDetailsService,
            Class<? extends GrantedAuthority> authorityClass) {
        this.userDetailsService = userDetailsService;
        this.authorityClass = authorityClass;
    }

    @Override
    @NonNull
    public UserDetailsMeta create(@NonNull UserDetails userDetails) {
        return new DefaultUserDetailsMeta(userDetails, this);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(UserDetails userDetails) {
        var typeHing = new TypeReference<Collection<? extends GrantedAuthority>>() {
            @Override
            public Type getType() {
                return new ParameterizedTypeImpl(new Type[] { authorityClass }, null, Collection.class);
            }
        };

        val authorityMeta = getMetaDataByHint(userDetails, UserDetailsMeta.AUTHORITY_META_KEY, typeHing);

        if (authorityMeta.isPresent()) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Load user's authorities from meta repository: {}", GsvcContextHolder.getRequestId(),
                        userDetails.getUsername());
            }
            return authorityMeta.get();
        }

        try {
            val ud = userDetailsService.loadUserByUsername(userDetails.getUsername());
            UserDetailsMeta.checkUserDetails(ud);

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
            log.warn("[{}] cannot load user's authorities: {} - {}", GsvcContextHolder.getRequestId(),
                    userDetails.getUsername(), e.getMessage());
        }
        return Collections.emptyList();
    }

}
