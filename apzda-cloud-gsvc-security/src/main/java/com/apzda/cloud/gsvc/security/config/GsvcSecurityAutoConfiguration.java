package com.apzda.cloud.gsvc.security.config;

import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.exception.ExceptionTransformer;
import com.apzda.cloud.gsvc.security.authentication.DeviceAwareAuthenticationProcessingFilter;
import com.apzda.cloud.gsvc.security.authorization.AsteriskPermissionEvaluator;
import com.apzda.cloud.gsvc.security.authorization.AuthorizationLogicCustomizer;
import com.apzda.cloud.gsvc.security.authorization.AuthorizeCustomizer;
import com.apzda.cloud.gsvc.security.authorization.PermissionChecker;
import com.apzda.cloud.gsvc.security.context.SpringSecurityUserProvider;
import com.apzda.cloud.gsvc.security.filter.AuthenticationExceptionFilter;
import com.apzda.cloud.gsvc.security.filter.MfaAuthenticationFilter;
import com.apzda.cloud.gsvc.security.handler.AuthenticationHandler;
import com.apzda.cloud.gsvc.security.handler.DefaultAuthenticationHandler;
import com.apzda.cloud.gsvc.security.mfa.MfaTokenCustomizer;
import com.apzda.cloud.gsvc.security.plugin.InjectCurrentUserPlugin;
import com.apzda.cloud.gsvc.security.repository.JwtContextRepository;
import com.apzda.cloud.gsvc.security.resolver.CurrentUserParamResolver;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import com.apzda.cloud.gsvc.security.token.JwtTokenManager;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.apzda.cloud.gsvc.security.userdetails.InMemoryUserDetailsMetaRepository;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@AutoConfiguration(before = {SecurityAutoConfiguration.class})
@ConditionalOnClass(DefaultAuthenticationEventPublisher.class)
@Import({RedisMetaRepoConfiguration.class, AuditorAutoConfiguration.class})
public class GsvcSecurityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityConfigProperties.class)
    @EnableMethodSecurity
    @EnableWebSecurity
    @RequiredArgsConstructor
    static class SecurityConfig {

        private final SecurityConfigProperties properties;

        private final ServiceConfigProperties svcProperties;

        private final ApplicationEventPublisher eventPublisher;

        private final ObjectProvider<List<DeviceAwareAuthenticationProcessingFilter>> authenticationProcessingFilter;

        private final ObjectProvider<List<AuthorizeCustomizer>> authorizeCustomizer;

        @Value("${server.error.path:/error}")
        private String errorPath;

        @Value("${apzda.cloud.security.role-prefix:ROLE_}")
        private String rolePrefix;

        @Bean
        @Order(-100)
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                AuthenticationManager authenticationManager,
                                                SecurityContextRepository securityContextRepository,
                                                AuthenticationHandler authenticationHandler)
            throws Exception {

            val requestCache = new NullRequestCache();
            http.requestCache(cache -> cache.requestCache(requestCache));

            http.sessionManagement((session) -> {
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                session.sessionAuthenticationStrategy(authenticationHandler);
                session.sessionAuthenticationFailureHandler(authenticationHandler);
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
            } else {
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
            // 用于处理Session加载过程中的异常
            http.addFilterBefore(new AuthenticationExceptionFilter(), SessionManagementFilter.class);

            if (properties.isMfaEnabled()) {
                val mfaAuthenticationFilter = new MfaAuthenticationFilter(properties.mfaExcludes());
                http.addFilterAfter(mfaAuthenticationFilter, ExceptionTranslationFilter.class);
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
                    if (StringUtils.isNotBlank(path)) {
                        var access = acl.getAccess();
                        if (StringUtils.isNotBlank(access)) {
                            access = access.replace("r(", "hasRole(")
                                .replace("a(", "hasAuthority(")
                                .replace("p(", "hasPermission(");
                            log.debug("ACL: {}(access={})", path, access);
                            authorize.requestMatchers(matcher).access(new WebExpressionAuthorizationManager(access));
                        } else {
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
            log.debug("SecurityFilterChain Initialized");
            return http.build();
        }

        @Bean
        @ConditionalOnMissingBean(GrantedAuthorityDefaults.class)
        static GrantedAuthorityDefaults grantedAuthorityDefaults(SecurityConfigProperties properties) {
            val rolePrefix = StringUtils.defaultIfBlank(properties.getRolePrefix(), "ROLE_");
            return new GrantedAuthorityDefaults(rolePrefix);
        }

        @Bean
        @ConditionalOnMissingBean
        AuthorizationLogicCustomizer authz() {
            return new AuthorizationLogicCustomizer();
        }

        @Bean
        AuthorizeCustomizer gtwAuthorizeCustomizer() {
            return new GatewayAuthorizeCustomizer();
        }

        @Bean("defaultAuthenticationProvider")
        @ConditionalOnMissingBean(name = "defaultAuthenticationProvider")
        AuthenticationProvider defaultAuthenticationProvider(UserDetailsService userDetailsService,
                                                             UserDetailsMetaRepository userDetailsMetaRepository,
                                                             PasswordEncoder passwordEncoder) {
            return new DefaultAuthenticationProvider(userDetailsService, userDetailsMetaRepository, passwordEncoder);
        }

        @Bean
        AuthenticationManager authenticationManager(ObjectPostProcessor<Object> objectPostProcessor,
                                                    ObjectProvider<List<AuthenticationProvider>> providers) throws
                                                                                                            Exception {
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
        AuthenticationHandler authenticationHandler(TokenManager tokenManager,
                                                    ObjectProvider<List<JwtTokenCustomizer>> customizers) {
            return new DefaultAuthenticationHandler(properties, tokenManager, customizers);
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
        @ConditionalOnProperty(name = "apzda.cloud.security.meta-repo", havingValue = "mem", matchIfMissing = true)
        UserDetailsMetaRepository userDetailsWrapper(UserDetailsMetaService userDetailsMetaService,
                                                     SecurityConfigProperties properties) {
            return new InMemoryUserDetailsMetaRepository(userDetailsMetaService, properties.getAuthorityClass());
        }

        @Bean
        @ConditionalOnMissingBean
        UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
            val manager = new InMemoryUserDetailsManager();
            val authorities = new ArrayList<GrantedAuthority>();

            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("view:/foo/info/user"));

            val password = "123456";
            val encodedPwd = passwordEncoder.encode(password);
            val user = User.withUsername("user")
                .password(encodedPwd)
                .authorities(authorities)
                .build();
            manager.createUser(user);

            authorities.add(new SimpleGrantedAuthority(rolePrefix + "ADMIN"));
            authorities.add(new SimpleGrantedAuthority("*:/foo/*"));

            val admin = User.withUsername("admin")
                .password(encodedPwd)
                .authorities(authorities)
                .build();

            manager.createUser(admin);
            log.warn("Default User, username: {}, password: {}, authorities: {}", "admin", password, authorities);

            val user1 = User.withUsername("user1")
                .password(encodedPwd)
                .authorities(authorities)
                .build();
            manager.createUser(user1);

            return manager;
        }

        @Bean
        @ConditionalOnMissingBean
        UserDetailsMetaService userDetailsMetaService(final UserDetailsService userDetailsService) {
            log.warn("Default UserDetailsMetaService is used, please use a real one!!!");

            return (userDetails) -> {
                if (!CollectionUtils.isEmpty(userDetails.getAuthorities())) {
                    return userDetails.getAuthorities();
                }
                log.trace("[{}] Load Authorities by userDetailsService.loadUserByUsername: {}",
                    GsvcContextHolder.getRequestId(), userDetails.getUsername());
                val ud = userDetailsService.loadUserByUsername(userDetails.getUsername());
                return ud.getAuthorities();
            };
        }

        @Bean
        InjectCurrentUserPlugin injectCurrentUserPlugin(SecurityConfigProperties properties) {
            return new InjectCurrentUserPlugin(properties);
        }

        @Bean
        @ConditionalOnMissingBean
        TokenManager tokenManager(UserDetailsService userDetailsService,
                                  UserDetailsMetaRepository userDetailsMetaRepository,
                                  JWTSigner jwtSigner,
                                  ObjectProvider<List<JwtTokenCustomizer>> customizers) {
            return new JwtTokenManager(userDetailsService, userDetailsMetaRepository, properties, jwtSigner, customizers);
        }

        @Bean
        @ConditionalOnProperty(name = "apzda.cloud.security.mfa-enabled", havingValue = "true")
        JwtTokenCustomizer mfaTokenCustomizer(SecurityConfigProperties properties) {
            return new MfaTokenCustomizer(properties);
        }

        @Bean
        @ConditionalOnMissingBean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(ApplicationContext applicationContext,
                                                                               PermissionEvaluator permissionEvaluator) {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            try {
                val roleHierarchy = applicationContext.getBean(RoleHierarchy.class);
                expressionHandler.setRoleHierarchy(roleHierarchy);
            } catch (Exception ignored) {
                log.debug("No RoleHierarchy found");
            }
            expressionHandler.setPermissionEvaluator(permissionEvaluator);
            return expressionHandler;
        }

        @Bean
        static ExceptionTransformer authExceptionTransformer() {
            return new ExceptionTransformer() {
                @Override
                public ErrorResponseException transform(Throwable exception) {
                    if (exception instanceof AccessDeniedException || exception instanceof LockedException
                        || exception instanceof AccountExpiredException
                        || exception instanceof CredentialsExpiredException) {
                        return new ErrorResponseException(HttpStatus.FORBIDDEN, exception);
                    }
                    return new ErrorResponseException(HttpStatus.UNAUTHORIZED, exception);
                }

                @Override
                public boolean supports(Class<? extends Throwable> eClass) {
                    return AuthenticationException.class.isAssignableFrom(eClass)
                        || AccessDeniedException.class.isAssignableFrom(eClass);
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean
        static PermissionEvaluator asteriskPermissionEvaluator(
            ObjectProvider<List<PermissionChecker>> checkerProvider) {
            return new AsteriskPermissionEvaluator(checkerProvider);
        }

        @Bean
        @ConditionalOnMissingBean
        CurrentUserProvider springSecurityCurrentUserProvider() {
            return new SpringSecurityUserProvider();
        }

    }

    @Configuration
    static class WebMvcConfigure implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new CurrentUserParamResolver());
        }
    }
}
