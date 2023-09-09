package com.apzda.cloud.gsvc.gtw;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
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

    public static GroupRoute valueOf(Route route) {
        val gr = new GroupRoute();
        gr.setPath(route.getPath());
        gr.index(route.index());
        gr.app(route.app());
        gr.setInterfaceName(route.getInterfaceName());
        gr.setLogin(route.getLogin());
        gr.method(route.getMethod());
        gr.setActions(route.getActions());
        gr.setFilters(route.getFilters());
        return gr;
    }

    private List<Route> routes = Collections.emptyList();

    @Override
    public String toString() {
        return super.toString();
    }

}
