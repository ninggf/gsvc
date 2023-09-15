/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Cookie;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author fengz windywany@gmail.com
 **/
@ConfigurationProperties("apzda.cloud.security")
@Data
public class SecurityConfigProperties {

    private boolean csrfEnabled;

    private boolean corsEnabled;

    private String tokenManager;

    private CookieConfig cookie = new CookieConfig();

    private String argName;

    private String tokenName = "Authorization";

    private String bearer = "Bearer";

    private String jwtKey;

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

}
