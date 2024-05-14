package com.apzda.cloud.gsvc.config;

import com.apzda.cloud.gsvc.gtw.GroupRoute;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author fengz
 */
@Data
@Validated
public class GatewayRouteConfig {

    private String prefix;

    private Boolean enabled = false;

    private final Set<String> filters = new HashSet<>();

    private final List<String> excludes = new ArrayList<>();

    /**
     * 南北流量路由。
     */
    private final List<GroupRoute> routes = new ArrayList<>();

}
