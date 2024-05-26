/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.token;

import org.springframework.lang.Nullable;

/**
 * @author fengz windywany@gmail.com
 **/

public interface JwtToken {

    void setUid(String uid);

    String getUid();

    void setName(String name);

    String getName();

    default void setRunAs(String runAs) {
    }

    @Nullable
    default String getRunAs() {
        return null;
    }

    void setProvider(String provider);

    String getProvider();

    void setAccessToken(String accessToken);

    String getAccessToken();

    void setRefreshToken(String RefreshToken);

    String getRefreshToken();

    void setMfa(String mfa);

    String getMfa();

    void setStatus(String status);

    @Nullable
    String getStatus();

    void setLocked(boolean locked);

    boolean isLocked();

    void setCredentialsExpired(boolean credentialsExpired);

    boolean isCredentialsExpired();

}
