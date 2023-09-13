package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.security.AuthorizeCustomizer;
import com.apzda.cloud.gsvc.security.filter.AuthenticationProcessingFilter;
import com.apzda.cloud.gsvc.security.handler.AuthenticationHandler;
import com.apzda.cloud.gsvc.security.handler.DefaultAuthenticationHandler;
import com.apzda.cloud.gsvc.security.repository.InMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.session.SessionManagementFilter;

import java.util.Comparator;
import java.util.List;

/**
 * @author fengz windywany@gmail.com
 */
@Slf4j
@AutoConfiguration(before = SecurityAutoConfiguration.class)
@ConditionalOnClass(DefaultAuthenticationEventPublisher.class)
public class GsvcSecurityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @RequiredArgsConstructor
    static class SecurityConfig {

        private final ApplicationEventPublisher eventPublisher;

        private final ObjectProvider<List<AuthenticationProcessingFilter>> authenticationProcessingFilter;

        private final ObjectProvider<List<AuthorizeCustomizer>> authorizeCustomizer;

        @Value("${server.error.path:/error}")
        private String errorPath;

        @Bean
        @Order(1)
        SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
                SecurityContextRepository securityContextRepository, AuthenticationHandler authenticationHandler)
                throws Exception {
            val requestCache = new NullRequestCache();
            http.requestCache(cache -> cache.requestCache(requestCache));

            http.sessionManagement((session) -> {
                // 不应开启http-session
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                // 认证策略
                session.sessionAuthenticationStrategy(authenticationHandler);
            });

            http.securityContext((context) -> {
                context.requireExplicitSave(true);
                context.securityContextRepository(securityContextRepository);
            });

            val filters = authenticationProcessingFilter.getIfAvailable();
            if (filters != null) {
                filters.sort(Comparator.comparingInt(AuthenticationProcessingFilter::getOrder));
                for (AbstractAuthenticationProcessingFilter filter : filters) {
                    filter.setSecurityContextHolderStrategy(SecurityContextHolder.getContextHolderStrategy());

                    filter.setAuthenticationManager(authenticationManager);
                    // 上下文仓储
                    filter.setSecurityContextRepository(securityContextRepository);
                    // 认证成功与失败处理器
                    filter.setAuthenticationSuccessHandler(authenticationHandler);
                    filter.setAuthenticationFailureHandler(authenticationHandler);
                    // 认证成功后处理策略（过期，禁止多处登录等）
                    filter.setSessionAuthenticationStrategy(authenticationHandler);

                    filter.setApplicationEventPublisher(eventPublisher);
                    filter.setAllowSessionCreation(false);

                    http.addFilterBefore(filter, SessionManagementFilter.class);
                }
            }

            // 在鉴权时发生的异常必须在这里处理（因为这个异常不会被ControllerAdvise捕获并处理）
            http.exceptionHandling((exception) -> {
                exception.accessDeniedHandler(authenticationHandler);
                exception.authenticationEntryPoint(authenticationHandler);
            });

            // 禁用自定义特征
            http.csrf(AbstractHttpConfigurer::disable);
            http.cors(AbstractHttpConfigurer::disable);
            http.anonymous(AbstractHttpConfigurer::disable);
            http.rememberMe(AbstractHttpConfigurer::disable);
            http.formLogin(AbstractHttpConfigurer::disable);
            http.httpBasic(AbstractHttpConfigurer::disable);
            http.logout(AbstractHttpConfigurer::disable);
            // URL 过滤
            String error = this.errorPath;
            http.authorizeHttpRequests((authorize) -> {
                val customizers = authorizeCustomizer.getIfAvailable();
                if (customizers != null) {
                    customizers.sort(Comparator.comparingInt(AuthorizeCustomizer::getOrder));
                    for (AuthorizeCustomizer customizer : customizers) {
                        customizer.customize(authorize);
                    }
                }
                authorize.requestMatchers(error).permitAll();
                authorize.anyRequest().permitAll();
            });
            log.info("SecurityFilterChain Initialized");
            return http.build();
        }

        @Bean
        AuthorizeCustomizer gtwAuthorizeCustomizer() {
            return new GatewayAuthorizeCustomizer();
        }

        @Bean
        AuthenticationManager authenticationManager(ObjectPostProcessor<Object> objectPostProcessor,
                ObjectProvider<List<AuthenticationProvider>> providers) throws Exception {
            // bookmark: 自定义认证管理器（可以自定义登录,相当于shiro中的Realm）
            val builder = new AuthenticationManagerBuilder(objectPostProcessor);
            if (providers.getIfAvailable() != null) {
                for (AuthenticationProvider authenticationProvider : providers.getIfAvailable()) {
                    builder.authenticationProvider(authenticationProvider);
                }
            }

            return builder.build();
        }

        @Bean
        @ConditionalOnMissingBean
        AuthenticationProvider authenticationProvider() {
            return new DefaultAuthenticationProvider();
        }

        @Bean
        @ConditionalOnMissingBean
        AuthenticationHandler authenticationHandler(GsvcExceptionHandler gsvcExceptionHandler) {
            // 认证处理器
            return new DefaultAuthenticationHandler(gsvcExceptionHandler);
        }

        @Bean
        @ConditionalOnMissingBean
        SecurityContextRepository securityContextRepository() {
            // bookmark: 自义存储 Context 仓储
            return new InMemoryRepository();
        }

    }

}
