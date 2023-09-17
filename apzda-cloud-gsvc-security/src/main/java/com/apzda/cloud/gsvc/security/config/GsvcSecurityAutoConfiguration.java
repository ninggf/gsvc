package com.apzda.cloud.gsvc.security.config;

import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.security.AuthorizeCustomizer;
import com.apzda.cloud.gsvc.security.TokenManager;
import com.apzda.cloud.gsvc.security.filter.AuthenticationProcessingFilter;
import com.apzda.cloud.gsvc.security.handler.AuthenticationHandler;
import com.apzda.cloud.gsvc.security.handler.DefaultAuthenticationHandler;
import com.apzda.cloud.gsvc.security.plugin.InjectCurrentUserPlugin;
import com.apzda.cloud.gsvc.security.repository.JwtContextRepository;
import com.apzda.cloud.gsvc.security.token.JwtTokenManager;
import com.apzda.cloud.gsvc.security.userdetails.InMemoryUserDetailsContainer;
import com.apzda.cloud.gsvc.security.userdetails.InMemoryUserServiceWrapper;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * @author fengz windywany@gmail.com
 */
@Slf4j
@AutoConfiguration(before = SecurityAutoConfiguration.class)
@ConditionalOnClass(DefaultAuthenticationEventPublisher.class)
@Import(RedisTokenManagerConfiguration.class)
public class GsvcSecurityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableConfigurationProperties(SecurityConfigProperties.class)
    @RequiredArgsConstructor
    static class SecurityConfig {

        private final SecurityConfigProperties properties;

        private final ApplicationEventPublisher eventPublisher;

        private final ObjectProvider<List<AuthenticationProcessingFilter>> authenticationProcessingFilter;

        private final ObjectProvider<List<AuthorizeCustomizer>> authorizeCustomizer;

        @Value("${server.error.path:/error}")
        private String errorPath;

        @Value("${apzda.cloud.config.logout-path:}")
        private String logoutPath;

        @Value("${apzda.cloud.config.home-page:/}")
        private String homePage;

        @Bean
        @Order(-100)
        SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
                SecurityContextRepository securityContextRepository, AuthenticationHandler authenticationHandler)
                throws Exception {
            val requestCache = new NullRequestCache();
            http.requestCache(cache -> cache.requestCache(requestCache));

            http.sessionManagement((session) -> {
                // 不应开启http-session
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                // 登录会话策略
                session.sessionAuthenticationStrategy(authenticationHandler);
                session.invalidSessionStrategy(authenticationHandler);
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
            // bookmark config logout
            if (StringUtils.isNotBlank(logoutPath)) {
                http.logout((logout) -> {
                    logout.logoutUrl(logoutPath);
                    if (StringUtils.isNotBlank(homePage)) {
                        logout.logoutSuccessUrl(homePage);
                    }

                    logout.addLogoutHandler(authenticationHandler);
                    logout.addLogoutHandler(new HeaderWriterLogoutHandler(
                            new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.ALL)));
                    logout.logoutSuccessHandler(authenticationHandler);
                    logout.clearAuthentication(true);
                    val cookie = properties.getCookie();
                    if (StringUtils.isNotBlank(cookie.getCookieName())) {
                        logout.deleteCookies(cookie.getCookieName());
                    }
                });
            }
            else {
                http.logout(AbstractHttpConfigurer::disable);
            }
            // URL 过滤
            String error = this.errorPath;
            http.authorizeHttpRequests((authorize) -> {
                val excludes = properties.getExclude();
                for (String exclude : excludes) {
                    authorize.requestMatchers(antMatcher(exclude)).permitAll();
                }

                val customizers = authorizeCustomizer.getIfAvailable();
                if (customizers != null) {
                    customizers.sort(Comparator.comparingInt(AuthorizeCustomizer::getOrder));
                    for (AuthorizeCustomizer customizer : customizers) {
                        customizer.customize(authorize);
                    }
                }

                authorize.requestMatchers(antMatcher(error)).permitAll();
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
        @ConditionalOnMissingBean
        AuthenticationProvider jwtAuthenticationProvider(UserDetailsService userDetailsService,
                UserDetailsWrapper userDetailsWrapper, PasswordEncoder passwordEncoder, TokenManager tokenManager) {
            return new DefaultAuthenticationProvider(userDetailsService, userDetailsWrapper, passwordEncoder,
                    tokenManager);
        }

        @Bean
        AuthenticationManager authenticationManager(ObjectPostProcessor<Object> objectPostProcessor,
                ObjectProvider<List<AuthenticationProvider>> providers) throws Exception {
            // bookmark: 自定义认证管理器
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
        AuthenticationHandler authenticationHandler(ServiceConfigProperties serviceConfigProperties,
                TokenManager tokenManager) {
            return new DefaultAuthenticationHandler(properties, tokenManager);
        }

        @Bean
        @ConditionalOnMissingBean
        SecurityContextRepository securityContextRepository(TokenManager tokenManager) {
            // bookmark: 自义存储 Context 仓储
            return new JwtContextRepository(tokenManager);
        }

        @Bean
        @ConditionalOnMissingBean
        JWTSigner gsvcJwtSigner() {
            val jwtKey = properties.getJwtKey();
            Assert.hasText(jwtKey, "apzda.cloud.security.jwt-key is not set");
            return JWTSignerUtil.hs256(jwtKey.getBytes());
        }

        @Bean
        @ConditionalOnMissingBean
        PasswordEncoder defaultGsvcPasswordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        @ConditionalOnMissingBean
        UserDetailsWrapper userDetailsWrapper(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                SecurityConfigProperties properties) {
            InMemoryUserDetailsContainer.init(properties);

            return new InMemoryUserServiceWrapper(userDetailsService);
        }

        @Bean
        @ConditionalOnMissingBean
        UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
            val manager = new InMemoryUserDetailsManager();
            val authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER"));
            val password = "123456";
            log.warn("Default User, username: {}, password: {}", "admin", password);
            val admin = User.withUsername("admin")
                .password(passwordEncoder.encode(password))
                .authorities(authorities)
                .build();
            manager.createUser(admin);
            return manager;
        }

        @Bean
        InjectCurrentUserPlugin injectCurrentUserPlugin() {
            return new InjectCurrentUserPlugin();
        }

        @Bean
        @ConditionalOnMissingBean
        TokenManager defaultTokenManager(UserDetailsService userDetailsService, UserDetailsWrapper userDetailsWrapper,
                JWTSigner jwtSigner) {
            return new JwtTokenManager(userDetailsService, userDetailsWrapper, properties, jwtSigner);
        }

    }

}