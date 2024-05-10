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

    String getName();

    void setName(String name);

    String getProvider();

    void setProvider(String provider);

    String getAccessToken();

    void setAccessToken(String accessToken);

    String getRefreshToken();

    void setRefreshToken(String RefreshToken);

    String getMfa();

    void setMfa(String mfa);

    @Nullable
    String getStatus();

    void setStatus(String status);

}
