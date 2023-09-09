package com.apzda.cloud.gsvc.gtw;

import cn.hutool.core.collection.CollectionUtil;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.google.common.base.Splitter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpMethod;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author fengz
 */
@Data
@Validated
public class Route {

    @NotBlank
    @NotNull
    private String path;

    private Boolean login;

    private Class<?> interfaceName;

    private String method;

    private List<HttpMethod> actions = Collections.emptyList();

    private Set<String> filters = new HashSet<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Route parent;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private int index;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private String app;

    public Route app(String app) {
        this.app = app;
        return this;
    }

    public String app() {
        return this.app;
    }

    public Route parent() {
        return this.parent;
    }

    public int index() {
        return this.index;
    }

    public Route parent(Route parent) {
        this.parent = parent;
        return this;
    }

    public Route index(int index) {
        this.index = index;
        return this;
    }

    public Route path(String path) {
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("path is blank!");
        }
        this.path = path;
        return this;
    }

    public Route login(String login) {
        if (login != null) {
            this.login = Boolean.parseBoolean(login);
        }
        else if (this.parent != null) {
            this.login = this.parent.login;
        }
        else {
            this.login = false;
        }
        return this;
    }

    public Route interfaceName(Class<?> clazz) {
        if (clazz != null) {
            this.interfaceName = clazz;
        }
        else if (this.parent != null && this.parent.interfaceName != null) {
            this.interfaceName = this.parent.interfaceName;
        }
        else if (this.parent != null) {
            // 子路由method不能为空
            throw new IllegalArgumentException(
                    String.format("Interface Name of apzda.cloud.gateway.%s.routes[%d].routes[%d] is blank", this.app,
                            this.parent.index, index));
        }
        return this;
    }

    public Route method(String method) {
        if (StringUtils.isNotBlank(method)) {
            this.method = method;
        }
        else if (this.parent != null) {
            // 子路由method不能为空
            throw new IllegalArgumentException(
                    String.format("Method of apzda.cloud.gateway.%s.routes[%d].routes[%d] is blank", this.app,
                            this.parent.index, index));
        }
        return this;
    }

    public Route actions(String actions) {
        if (StringUtils.isNotBlank(actions)) {
            this.actions = Splitter.on(",")
                .omitEmptyStrings()
                .trimResults()
                .splitToList(actions)
                .stream()
                .map(action -> HttpMethod.valueOf(action.toUpperCase()))
                .toList();
            if (CollectionUtil.isEmpty(this.actions)) {
                this.actions = List.of(HttpMethod.GET, HttpMethod.POST);
            }
        }
        else if (this.parent != null) {
            this.actions = this.parent.actions;
        }
        else {
            this.actions = List.of(HttpMethod.POST);
        }
        return this;
    }

    public Route filters(String filters) {
        if (StringUtils.isNotBlank(filters)) {
            this.filters = new HashSet<>(Splitter.on(",").omitEmptyStrings().trimResults().splitToList(filters));
        }
        return this;
    }

    @Override
    public String toString() {
        val str = new ToStringCreator(this);
        if (parent != null) {
            str.append("path", parent.path + path);
        }
        else {
            str.append("path", path);
        }
        val svcName = GatewayServiceRegistry.svcName(interfaceName);
        str.append("svc", svcName)
            .append("method", method)
            .append("index", index)
            .append("login", login)
            .append("actions", actions)
            .append("filters", filters);
        return str.toString();
    }

}
