package com.apzda.cloud.gsvc.security.token;

import cn.hutool.jwt.signers.JWTSigner;
import com.apzda.cloud.gsvc.security.JwtToken;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author fengz
 */
@Slf4j
public class RedisTokenManager extends DefaultTokenManager {

    private final StringRedisTemplate redisTemplate;

    public RedisTokenManager(StringRedisTemplate redisTemplate, SecurityConfigProperties properties,
            JWTSigner jwtSigner) {
        super(properties, jwtSigner);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public AuthenticationToken restore(HttpServletRequest request) {
        return null;
    }

    @Override
    public JwtToken createJwtToken(AuthenticationToken authentication) {
        return null;
    }

    @Override
    public JwtToken refresh(JwtToken token, AuthenticationToken authentication) {
        return null;
    }

}
