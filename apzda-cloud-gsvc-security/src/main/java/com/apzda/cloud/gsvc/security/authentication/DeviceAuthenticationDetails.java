package com.apzda.cloud.gsvc.security.authentication;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Optional;

/**
 * @author fengz
 */
public class DeviceAuthenticationDetails extends WebAuthenticationDetails {

    private final String device;

    private final String deviceId;

    private final String osName;

    private final String osVer;

    private final String appVer;

    public DeviceAuthenticationDetails(HttpServletRequest request) {
        super(request);
        val headers = GsvcContextHolder.headers();
        this.device = StringUtils.defaultString(headers.get("X-Device"), "pc");
        this.deviceId = StringUtils.defaultString(headers.get("X-Device-Id"));
        this.osName = StringUtils.defaultString(headers.get("X-OS-Name"));
        this.osVer = StringUtils.defaultString(headers.get("X-OS-Ver"));
        this.appVer = StringUtils.defaultString(headers.get("X-App-Ver"));
    }

    public static Optional<DeviceAuthenticationDetails> create() {
        val request = GsvcContextHolder.getRequest();
        return request.map(DeviceAuthenticationDetails::new);
    }

    public String getDevice() {
        return device;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVer() {
        return osVer;
    }

    public String getAppVer() {
        return appVer;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("ip", getRemoteAddress())
            .append("device", device)
            .append("deviceId", deviceId)
            .append("os", osName)
            .append("osVer", osVer)
            .append("appVer", appVer)
            .toString();
    }

}
