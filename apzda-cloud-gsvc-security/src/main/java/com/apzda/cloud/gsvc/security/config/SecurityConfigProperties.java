/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.config;

import com.apzda.cloud.gsvc.security.captcha.CaptchaProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Cookie;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fengz windywany@gmail.com
 **/
@ConfigurationProperties("apzda.cloud.security")
@Data
public class SecurityConfigProperties {

    private CookieConfig cookie = new CookieConfig();

    private CaptchaConfig captcha = new CaptchaConfig();

    private String argName;

    private String tokenName = "Authorization";

    private String bearer = "Bearer";

    private String jwtKey;

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
    public static class CaptchaConfig {

        private boolean enabled;

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration timeout = Duration.ofMinutes(30);

        private Class<? extends CaptchaProvider> provider;

        private Map<String, String> props = new HashMap<>();

    }

    @Data
    @Validated
    public static class ACL {

        @NotBlank
        private String path;

        private String access;

    }

}
