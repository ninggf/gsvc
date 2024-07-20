package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.utils.StringUtils;
import lombok.Data;
import lombok.ToString;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.style.ToStringCreator;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fengz
 */
@Data
@ToString
@ConfigurationProperties("apzda.cloud")
public final class ServiceConfigProperties {

    public final static ServiceConfigProperties EMPTY = new ServiceConfigProperties();

    private final static ServiceConfig SERVICE_DEFAULT = new ServiceConfig();

    private final static ServiceConfig REFERENCE_DEFAULT = new ServiceConfig();

    private final Map<String, ServiceConfig> service = new LinkedHashMap<>();

    private final Map<String, ServiceConfig> reference = new LinkedHashMap<>();

    private final Map<String, GatewayRouteConfig> gateway = new LinkedHashMap<>();

    private final Config config = new Config();

    private final ModemConfig modem = new ModemConfig();

    private final Registry registry = new Registry();

    private final MybatisPlus mybatisPlus = new MybatisPlus();

    public ServiceConfig svcConfig(String name) {
        return svcConfig(name, service, SERVICE_DEFAULT);
    }

    public ServiceConfig refConfig(String name) {
        return svcConfig(name, reference, REFERENCE_DEFAULT);
    }

    private ServiceConfig svcConfig(String name, Map<String, ServiceConfig> source, ServiceConfig defCfg) {
        // BarService > bar-service > barService
        if (!source.containsKey(name)) {
            val dName = StringUtils.toDashed(name);
            if (source.containsKey(dName)) {
                source.put(name, source.get(dName));
                return source.get(name);
            }
            val lName = StringUtils.lowerFirst(name);
            if (source.containsKey(lName)) {
                source.put(name, source.get(lName));
                return source.get(name);
            }
            else {
                source.put(name, defCfg);
            }
        }
        return source.get(name);
    }

    public enum RegistryType {

        NONE, DOCKER, K8S, EUREKA, NACOS

    }

    public enum NameStyle {

        CAMEL, DASHED, KEBAB;

    }

    /**
     * @author fengz
     */
    @Data
    public static class Config {

        private String loginPage;

        private String logoutPath;

        private String homePage;

        private String realIpHeader = "X-Real-IP";

        private String realIpFrom;

        private String dictItemTable = "sys_dict_item";

        private String dictCodeColumn = "dict_code";

        private String dictLabelColumn = "dict_label";

        private String dictValueColumn = "dict_value";

        private String dictDeletedColumn;

        private String dictNotDeletedValue;

        private String dictLabelSuffix = "Text";

        private String pageNumber = "pageNumber";

        private String pageSize = "pageSize";

        private String pageSorts = "pageSorts";

        @DurationUnit(ChronoUnit.HOURS)
        private Duration tempExpireTime = Duration.ofHours(168);

        private boolean acceptLiteralFieldNames = true;

        private boolean properUnsignedNumberSerialization = true;

        private boolean serializeLongsAsString = true;

        private boolean contextCapture = false;

        private boolean flatResponse = false;

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

    @Data
    public static final class MybatisPlus {

        private String tenantIdColumn = "tenant_id";

        private boolean disableTenantPlugin = true;

        private List<String> createdAtColumns = new ArrayList<>(List.of("createdAt"));

        private List<String> createdByColumns = new ArrayList<>(List.of("createdBy"));

        private List<String> updatedAtColumns = new ArrayList<>(List.of("updatedAt"));

        private List<String> updatedByColumns = new ArrayList<>(List.of("updatedBy"));

    }

    @Data
    public static final class Registry {

        private RegistryType type = RegistryType.NONE;

        private NameStyle nameStyle = NameStyle.CAMEL;

        private int port = 8080;

        private boolean ssl = false;

    }

    @Data
    public static final class ModemConfig {

        private Algorithm algorithm = Algorithm.AES;

        private String mode = "CBC";

        private String padding = "PKCS5Padding";

        private String iv = "0102030405060708";

        private String key = "0CoJUm6Qyw8W8jud";

    }

    public enum Algorithm {

        AES, DES

    }

}
