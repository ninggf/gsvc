/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.server.Cookie;

/**
 * @author fengz windywany@gmail.com
 **/
@ConfigurationProperties("apzda.cloud.security")
@Data
public class SecurityConfigProperties {

    private boolean csrfEnabled;

    private boolean corsEnabled;

    private CookieConfig cookie = new CookieConfig();

    private String tokenName = "Authorization";

    private String bearer = "Bearer";

    private String jwtKey;

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
