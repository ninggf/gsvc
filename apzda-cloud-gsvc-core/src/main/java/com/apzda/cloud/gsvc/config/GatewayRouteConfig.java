package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.gtw.GroupRoute;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fengz
 */
@Data
@Validated
public class GatewayRouteConfig {

    private Class<?> interfaceName;

    /**
     * 南北流量路由。
     */
    private final List<GroupRoute> routes = new ArrayList<>();

}
