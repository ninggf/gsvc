package com.apzda.cloud.gsvc.security.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;

/**
 * @author fengz
 */
public class DeviceAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, DeviceAuthenticationDetails> {

    /**
     * @param context the {@code HttpServletRequest} object.
     * @return the {@code DeviceAuthenticationDetails} containing information about the
     * current request
     */
    @Override
    public DeviceAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new DeviceAuthenticationDetails(context);
    }

}
