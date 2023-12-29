/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Cookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fengz windywany@gmail.com
 **/
@ConfigurationProperties("apzda.cloud.security")
@Data
public class SecurityConfigProperties {

    private String metaRepo;

    private boolean traceEnabled;

    private Class<? extends GrantedAuthority> authorityClass = SimpleGrantedAuthority.class;

    private CookieConfig cookie = new CookieConfig();

    private String argName;

    private String tokenName = "Authorization";

    private String bearer = "Bearer";

    private String jwtKey = "123456789";

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration jwtLeeway = Duration.ofSeconds(30);

    private List<String> exclude = new ArrayList<>();

    private List<ACL> acl = new ArrayList<>();

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration accessTokenTimeout = Duration.ofMinutes(5);

    @DurationUnit(ChronoUnit.DAYS)
    private Duration refreshTokenTimeout = Duration.ofDays(365);

    public String getTokenName() {
        return StringUtils.defaultIfBlank(tokenName, "Authorization");
    }

    @Data
    public static class CookieConfig {

        private String cookieName;

        private String cookieDomain;

        private boolean cookieSecurity;

        private String cookiePath = "/";

        private Cookie.SameSite sameSite = Cookie.SameSite.STRICT;

        private int maxAge = -1;

    }

    @Data
    @Validated
    public static class ACL {

        @NotBlank
        private String path;

        private String access;

    }

}
