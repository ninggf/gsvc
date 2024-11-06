package com.apzda.cloud.gsvc.security.userdetails;

import com.apzda.cloud.gsvc.security.jackson.SimpleGrantedAuthorityDeserializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.net.SocketException;
import java.util.Optional;

/**
 * @author fengz
 */
@Slf4j
public class RedisUserDetailsMetaRepository extends AbstractUserDetailsMetaRepository {

    private static final String META_KEY_PREFIX = "user.meta.";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public RedisUserDetailsMetaRepository(UserDetailsMetaService userDetailsMetaService,
            StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
            Class<? extends GrantedAuthority> authorityClass) {
        super(userDetailsMetaService, authorityClass);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        SimpleModule module = new SimpleModule();
        module.addDeserializer(SimpleGrantedAuthority.class, new SimpleGrantedAuthorityDeserializer());
        this.objectMapper.registerModule(module);
    }

    @Override
    public void setMetaData(UserDetails userDetails, String key, Object value) {
        try {
            redisTemplate.<String, String>opsForHash()
                .put(thenMetaKey(userDetails), key, objectMapper.writeValueAsString(value));
        }
        catch (Exception e) {
            log.error("Cannot set user meta: {}.{} = {}", thenMetaKey(userDetails), key, value, e);
        }
    }

    @Override
    public void removeMetaData(UserDetails userDetails, String key) {
        try {
            redisTemplate.opsForHash().delete(thenMetaKey(userDetails), key);
        }
        catch (Exception e) {
            log.error("Cannot remove user meta: {}.{}", thenMetaKey(userDetails), key, e);
        }
    }

    @Override
    public void removeMetaData(UserDetails userDetails) {
        try {
            redisTemplate.delete(thenMetaKey(userDetails));
        }
        catch (Exception e) {
            log.error("Cannot remove user meta: {}", thenMetaKey(userDetails), e);
        }
    }

    @Override
    @NonNull
    public <R> Optional<R> getCachedMetaData(UserDetails userDetails, String key, String metaKey, Class<R> rClass) {
        try {
            val value = get(userDetails, key);
            if (value != null) {
                if (log.isTraceEnabled()) {
                    log.trace("User meta '{}' of '{}' loaded from Redis", key, userDetails.getUsername());
                }
                return Optional.of(objectMapper.readValue(value, rClass));
            }
        }
        catch (Exception e) {
            log.error("Cannot load user meta for {}.{} - {}", thenMetaKey(userDetails), key, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    @NonNull
    public <R> Optional<R> getCachedMetaData(UserDetails userDetails, String key, String metaKey,
            TypeReference<R> typeReference) {
        try {
            val value = get(userDetails, key);
            if (value != null) {
                if (log.isTraceEnabled()) {
                    log.trace("User metas '{}' of '{}' loaded from Redis", key, userDetails.getUsername());
                }
                return Optional.of(objectMapper.readValue(value, typeReference));
            }
        }
        catch (Exception e) {
            log.error("Cannot load user metas for {}.{} - {}", thenMetaKey(userDetails), key, e.getMessage());
        }
        return Optional.empty();
    }

    @Nonnull
    protected String thenMetaKey(@Nonnull UserDetails userDetails) {
        return META_KEY_PREFIX + userDetails.getUsername();
    }

    @Nullable
    private String get(UserDetails userDetails, String key) {
        val metaKey = thenMetaKey(userDetails);
        try {
            return redisTemplate.<String, String>opsForHash().get(metaKey, key);
        }
        catch (RedisSystemException e) {
            val rc = e.getRootCause();
            if (rc instanceof SocketException) {
                // try again
                return redisTemplate.<String, String>opsForHash().get(metaKey, key);
            }
        }

        return null;
    }

}
