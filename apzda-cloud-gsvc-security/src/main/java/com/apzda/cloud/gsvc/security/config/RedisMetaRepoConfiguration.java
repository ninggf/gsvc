package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.security.userdetails.RedisUserDetailsMetaRepository;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author fengz
 */
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisMetaRepoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "apzda.cloud.security.meta-repo", havingValue = "redis")
    UserDetailsMetaRepository redisUserDetailsMetaRepository(UserDetailsMetaService userDetailsMetaService,
                                                             StringRedisTemplate redisTemplate, ObjectMapper objectMapper, SecurityConfigProperties properties) {
        return new RedisUserDetailsMetaRepository(userDetailsMetaService, redisTemplate, objectMapper,
            properties.getAuthorityClass());
    }

}
