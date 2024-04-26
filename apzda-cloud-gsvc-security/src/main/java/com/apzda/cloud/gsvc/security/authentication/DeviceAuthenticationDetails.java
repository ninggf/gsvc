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
public class DeviceAuthenticationDetails extends WebAuthenticationDetails implements AuthenticationDetails {

    private final String device;

    private final String deviceId;

    private final String osName;

    private final String osVer;

    private final String app;

    private final Map<String, String> appMeta;

    public DeviceAuthenticationDetails(HttpServletRequest request) {
        super(GsvcContextHolder.getRemoteIp(), null);
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

    public static DeviceAuthenticationDetails create(HttpServletRequest request) {
        return new DeviceAuthenticationDetails(request);
    }

    public AuthenticationDetails toGeneric() {
        val details = new GenericAuthenticationDetails();
        details.setApp(app);
        details.setAppMeta(appMeta);
        details.setDevice(device);
        details.setDeviceId(deviceId);
        details.setOsName(osName);
        details.setOsVer(osVer);
        details.setRemoteAddress(getRemoteAddress());

        return details;
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
