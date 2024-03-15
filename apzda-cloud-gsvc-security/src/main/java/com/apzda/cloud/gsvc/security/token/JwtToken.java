/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.token;

/**
 * @author fengz windywany@gmail.com
 **/

public interface JwtToken {
    String getName();

    void setName(String name);

    String getAccessToken();

    void setAccessToken(String accessToken);

    String getRefreshToken();

    void setRefreshToken(String RefreshToken);
}
