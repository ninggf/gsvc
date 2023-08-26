package com.apzda.cloud.gsvc.gtw;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

/**
 * @author fengz
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Validated
public class GroupRoute extends Route {

    public GroupRoute() {
    }

    public GroupRoute(Route route) {
        this.setPath(route.getPath());
        this.setServiceIndex(route.getServiceIndex());
        this.setLogin(route.getLogin());
        this.method(route.getMethod());
        this.setActions(route.getActions());
        this.setFilters(route.getFilters());
    }

    private List<Route> routes = Collections.emptyList();

}
