package com.apzda.cloud.gsvc.security.config;

import cn.hutool.jwt.signers.JWTSigner;
import com.apzda.cloud.gsvc.security.TokenManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author fengz
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisTokenManagerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "apzda.cloud.security.token-manager", havingValue = "redis")
    TokenManager redisTokenManager(StringRedisTemplate redisTemplate, SecurityConfigProperties properties,
            JWTSigner jwtSigner) {
        return null;
    }

}
