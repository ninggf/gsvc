/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.security.token.JwtToken;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Cookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private List<ACL> acl = new ArrayList<>();

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration accessTokenTimeout = Duration.ofMinutes(5);

    @DurationUnit(ChronoUnit.DAYS)
    private Duration refreshTokenTimeout = Duration.ofDays(365);

    public String getTokenName() {
        return StringUtils.defaultIfBlank(tokenName, "Authorization");
    }

    public Set<RequestMatcher> mfaExcludes() {
        val mfaExcludeSet = mfaExclude.stream().map(AntPathRequestMatcher::antMatcher).collect(Collectors.toSet());
        val excludeSet = exclude.stream().map(AntPathRequestMatcher::antMatcher).collect(Collectors.toSet());
        val excludes = new HashSet<RequestMatcher>(mfaExcludeSet.size() + excludeSet.size());
        excludes.addAll(mfaExcludeSet);
        excludes.addAll(excludeSet);
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

}
