package com.apzda.cloud.gsvc.gtw;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

/**
 * @author fengz
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class GroupRoute extends Route {

    public GroupRoute() {
    }

    public GroupRoute(Route route) {
        this.setPath(route.getPath());
        this.index(route.index());
        this.app(route.app());
        this.setInterfaceName(route.getInterfaceName());
        this.setLogin(route.getLogin());
        this.method(route.getMethod());
        this.setActions(route.getActions());
        this.setFilters(route.getFilters());
    }

    private List<Route> routes = Collections.emptyList();

    @Override
    public String toString() {
        return super.toString();
    }

}
