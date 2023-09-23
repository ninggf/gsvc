package com.apzda.cloud.gsvc.gtw;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz
 */
@Data
@Accessors(chain = true)
public class RouteMeta {

    private boolean login;

    private String access;

    public RouteMeta setAccess(String access) {
        if (StringUtils.isNotBlank(access)) {
            this.access = access.replace("r(", "hasRole(")
                .replace("a(", "hasAuthority(")
                .replace("p(", "hasPermission(");
            this.login = true;
        }
        return this;
    }

}
