package com.apzda.cloud.gsvc.security.authentication;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Map;

/**
 * @author fengz
 */
@Getter
public class DeviceAuthenticationDetails extends WebAuthenticationDetails {

    private final String device;

    private final String deviceId;

    private final String osName;

    private final String osVer;

    private final String app;

    private final Map<String, String> appMeta;

    public DeviceAuthenticationDetails(HttpServletRequest request) {
        super(request);
        val headers = GsvcContextHolder.headers();
        this.device = StringUtils.defaultIfBlank(headers.get("X-Device"), "pc");
        this.deviceId = StringUtils.defaultString(headers.get("X-Device-Id"));
        this.osName = StringUtils.defaultString(headers.get("X-OS-Name"));
        this.osVer = StringUtils.defaultString(headers.get("X-OS-Ver"));
        this.app = StringUtils.defaultIfBlank(headers.get("X-App"), "web");
        this.appMeta = GsvcContextHolder.headers("X-App-");
    }

    public static DeviceAuthenticationDetails create() {
        val request = GsvcContextHolder.getRequest();
        return request.map(DeviceAuthenticationDetails::new).orElse(null);
    }

    @Override
    public String toString() {
        return new ToStringCreator(this).append("ip", getRemoteAddress())
            .append("device", device)
            .append("deviceId", deviceId)
            .append("os", osName)
            .append("osVer", osVer)
            .append("app", app)
            .append("appMeta", appMeta)
            .toString();
    }

}
