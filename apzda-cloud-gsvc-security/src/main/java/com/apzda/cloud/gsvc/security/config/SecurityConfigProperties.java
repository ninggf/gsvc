/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.security.token.JwtToken;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Cookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fengz windywany@gmail.com
 **/
@ConfigurationProperties("apzda.cloud.security")
@Data
public class SecurityConfigProperties {

    private String metaRepo;

    private boolean traceEnabled;

    private boolean mfaEnabled;

    private boolean accountLockedEnabled;

    private boolean bindEnabled = true;

    private boolean credentialsExpiredEnabled;

    private String rolePrefix = "ROLE_";

    private Class<? extends GrantedAuthority> authorityClass = SimpleGrantedAuthority.class;

    private CookieConfig cookie = new CookieConfig();

    private String argName;

    private String tokenName = "Authorization";

    private String bearer = "Bearer";

    private String jwtKey = "123456789";

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration jwtLeeway = Duration.ofSeconds(30);

    private List<String> exclude = new ArrayList<>();

    private List<String> mfaExclude = new ArrayList<>();

    private List<String> activePath = new ArrayList<>();

    private List<String> bindPath = new ArrayList<>();

    private List<String> resetCredentialsPath = new ArrayList<>();

    private List<ACL> acl = new ArrayList<>();

    private Map<String, CorsConfig> cors = new LinkedHashMap<>();

    private HeadersConfig headers = new HeadersConfig();

    private List<String> allowedDevices = new ArrayList<>();

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration accessTokenTimeout = Duration.ofMinutes(5);

    @DurationUnit(ChronoUnit.DAYS)
    private Duration refreshTokenTimeout = Duration.ofDays(365);

    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private Set<RequestMatcher> excludeSet;

    public String getTokenName() {
        return StringUtils.defaultIfBlank(tokenName, "Authorization");
    }

    public Set<RequestMatcher> excludes() {
        if (excludeSet == null) {
            excludeSet = antMatchers(exclude);
        }
        return excludeSet;
    }

    public Set<RequestMatcher> mfaExcludes() {
        return antMatchers(mfaExclude);
    }

    public Set<RequestMatcher> activeExcludes() {
        return antMatchers(activePath);
    }

    public Set<RequestMatcher> resetCredentialsExcludes() {
        return antMatchers(resetCredentialsPath);
    }

    public Set<RequestMatcher> bindExcludes() {
        return antMatchers(bindPath);
    }

    public boolean deviceIsAllowed(String device) {
        if (StringUtils.isBlank(device)) {
            return false;
        }

        if (CollectionUtils.isEmpty(allowedDevices)) {
            return true;
        }

        return allowedDevices.contains(device);
    }

    static Set<RequestMatcher> antMatchers(List<String> paths) {
        val excludePaths = paths.stream().map(AntPathRequestMatcher::antMatcher).collect(Collectors.toSet());
        val excludes = new HashSet<RequestMatcher>(excludePaths.size());
        excludes.addAll(excludePaths);
        return excludes;
    }

    @Data
    public static class CookieConfig {

        private String cookieName;

        private String cookieDomain;

        private boolean cookieSecurity;

        private String cookiePath = "/";

        private Cookie.SameSite sameSite = Cookie.SameSite.STRICT;

        private int maxAge = -1;

        public jakarta.servlet.http.Cookie createCookie(JwtToken jwtToken) {
            val cookieName = getCookieName();
            val accessToken = jwtToken.getAccessToken();
            val cookie = new jakarta.servlet.http.Cookie(cookieName, accessToken);
            cookie.setDomain(this.getCookieDomain());
            cookie.setHttpOnly(true);
            cookie.setSecure(this.isCookieSecurity());
            cookie.setPath(this.getCookiePath());
            cookie.setMaxAge(this.getMaxAge());
            cookie.setAttribute("SameSite", this.getSameSite().attributeValue());
            return cookie;
        }

    }

    @Data
    @Validated
    public static class ACL {

        @NotBlank
        private String path;

        private String access;

    }

    @Data
    public static class CorsConfig {

        private List<String> headers;

        private List<String> exposed;

        private Boolean credentials;

        private List<String> origins;

        private List<String> originPatterns;

        private Boolean allowPrivateNetwork;

        private List<String> methods;

        private Duration maxAge;

    }

    @Data
    public static class HeadersConfig {

        private boolean hsts = false;

        private boolean xss = false;

        private boolean frame = false;

        private boolean contentType = false;

    }

}
