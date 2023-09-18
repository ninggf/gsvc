package com.apzda.cloud.gsvc.security.config;

import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.security.AuthorizeCustomizer;
import com.apzda.cloud.gsvc.security.TokenManager;
import com.apzda.cloud.gsvc.security.authentication.DeviceAwareAuthenticationProcessingFilter;
import com.apzda.cloud.gsvc.security.handler.AuthenticationHandler;
import com.apzda.cloud.gsvc.security.handler.DefaultAuthenticationHandler;
import com.apzda.cloud.gsvc.security.plugin.InjectCurrentUserPlugin;
import com.apzda.cloud.gsvc.security.repository.JwtContextRepository;
import com.apzda.cloud.gsvc.security.token.JwtTokenManager;
import com.apzda.cloud.gsvc.security.userdetails.InMemoryUserServiceMetaRepository;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
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
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
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
public class GsvcSecurityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableConfigurationProperties(SecurityConfigProperties.class)
    @RequiredArgsConstructor
    static class SecurityConfig {

        private final SecurityConfigProperties properties;

        private final ServiceConfigProperties svcProperties;

        private final ApplicationEventPublisher eventPublisher;

        private final ObjectProvider<List<DeviceAwareAuthenticationProcessingFilter>> authenticationProcessingFilter;

        private final ObjectProvider<List<AuthorizeCustomizer>> authorizeCustomizer;

        @Value("${server.error.path:/error}")
        private String errorPath;

        @Bean
        @Order(-100)
        SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
                SecurityContextRepository securityContextRepository, AuthenticationHandler authenticationHandler)
                throws Exception {

            val requestCache = new NullRequestCache();
            http.requestCache(cache -> cache.requestCache(requestCache));

            http.sessionManagement((session) -> {
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                session.sessionAuthenticationStrategy(authenticationHandler);
                session.invalidSessionStrategy(authenticationHandler);
            });

            http.securityContext((context) -> {
                context.requireExplicitSave(true);
                context.securityContextRepository(securityContextRepository);
            });

            http.csrf(AbstractHttpConfigurer::disable);
            http.cors(AbstractHttpConfigurer::disable);
            http.anonymous(AbstractHttpConfigurer::disable);
            http.rememberMe(AbstractHttpConfigurer::disable);
            http.formLogin(AbstractHttpConfigurer::disable);
            http.httpBasic(AbstractHttpConfigurer::disable);

            val logoutPath = svcProperties.getConfig().getLogoutPath();
            if (StringUtils.isNotBlank(logoutPath)) {
                log.debug("Visit {} to logout!", logoutPath);
                http.logout((logout) -> {
                    logout.logoutUrl(logoutPath);

                    val homePage = svcProperties.getConfig().getHomePage();
                    if (StringUtils.isNotBlank(homePage)) {
                        log.debug("Redirect to {} when logout", homePage);
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

            val filters = authenticationProcessingFilter.getIfAvailable();
            if (filters != null) {
                filters.sort(Comparator.comparingInt(DeviceAwareAuthenticationProcessingFilter::getOrder));
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

            http.exceptionHandling((exception) -> {
                exception.accessDeniedHandler(authenticationHandler);
                exception.authenticationEntryPoint(authenticationHandler);
            });

            String error = this.errorPath;
            http.authorizeHttpRequests((authorize) -> {
                val excludes = properties.getExclude();
                log.debug("ACL: Permit{}", excludes);
                for (String exclude : excludes) {
                    authorize.requestMatchers(antMatcher(exclude)).permitAll();
                }

                val aclLists = properties.getAcl();
                for (SecurityConfigProperties.ACL acl : aclLists) {
                    val path = acl.getPath();
                    val matcher = antMatcher(path);
                    var access = acl.getAccess();
                    if (StringUtils.isNotBlank(path)) {
                        if (StringUtils.isNotBlank(access)) {
                            access = access.replace("r(", "hasRole(").replace("p(", "hasAuthority(");
                            log.debug("ACL: {}(access={})", path, access);
                            authorize.requestMatchers(matcher).access(new WebExpressionAuthorizationManager(access));
                        }
                        else {
                            log.debug("ACL: {}", path);
                            authorize.requestMatchers(matcher).authenticated();
                        }
                    }
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
                UserDetailsMetaRepository userDetailsMetaRepository, PasswordEncoder passwordEncoder,
                TokenManager tokenManager) {
            return new DefaultAuthenticationProvider(userDetailsService, userDetailsMetaRepository, passwordEncoder,
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
        UserDetailsMetaRepository userDetailsWrapper(UserDetailsService userDetailsService) {
            return new InMemoryUserServiceMetaRepository(userDetailsService);
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
        InjectCurrentUserPlugin injectCurrentUserPlugin(SecurityConfigProperties properties) {
            return new InjectCurrentUserPlugin(properties);
        }

        @Bean
        @ConditionalOnMissingBean
        TokenManager defaultTokenManager(UserDetailsService userDetailsService,
                UserDetailsMetaRepository userDetailsMetaRepository, JWTSigner jwtSigner) {
            return new JwtTokenManager(userDetailsService, userDetailsMetaRepository, properties, jwtSigner);
        }

    }

}
