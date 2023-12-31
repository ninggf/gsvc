package com.apzda.cloud.gsvc.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.style.ToStringCreator;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author fengz
 */
@Data
@ToString
@ConfigurationProperties("apzda.cloud")
public class ServiceConfigProperties {

    private final static ServiceConfig SERVICE_DEFAULT = new ServiceConfig();

    private final static ServiceConfig REFERENCE_DEFAULT = new ServiceConfig();

    private final Map<String, ServiceConfig> service = new LinkedHashMap<>();

    private final Map<String, ServiceConfig> reference = new LinkedHashMap<>();

    private final Map<String, GatewayRouteConfig> gateway = new LinkedHashMap<>();

    private Config config;

    public Config getConfig() {
        return config == null ? new Config() : config;
    }

    public ServiceConfig svcConfig(String name) {
        return service.getOrDefault(name, SERVICE_DEFAULT);
    }

    public ServiceConfig refConfig(String name) {
        return reference.getOrDefault(name, REFERENCE_DEFAULT);
    }

    /**
     * @author fengz
     */
    @Data
    public static class Config {

        private String loginPage;

        private String logoutPath;

        private String homePage;

        @DurationUnit(ChronoUnit.HOURS)
        private Duration tempExpireTime = Duration.ofHours(24);

        private boolean acceptLiteralFieldNames = true;

        private boolean properUnsignedNumberSerialization = true;

        private boolean serializeLongsAsString = true;

        @Override
        public String toString() {
            return new ToStringCreator(this).append("Login", loginPage)
                .append("Logout", logoutPath)
                .append("home", homePage)
                .append("acceptLiteralFieldNames", acceptLiteralFieldNames)
                .append("properUnsignedNumberSerialization", properUnsignedNumberSerialization)
                .append("serializeLongsAsString", serializeLongsAsString)
                .toString();
        }

    }

}
