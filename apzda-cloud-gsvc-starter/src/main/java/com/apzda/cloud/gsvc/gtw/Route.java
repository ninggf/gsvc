package com.apzda.cloud.gsvc.gtw;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.base.Splitter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

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

    private Integer serviceIndex;

    private String method;

    private List<HttpMethod> actions = Collections.emptyList();

    private List<String> filters = Collections.emptyList();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Route parent;

    public Route parent(Route parent) {
        this.parent = parent;
        return this;
    }

    public Route path(String path) {
        if (StringUtils.isBlank(path)) {
            throw new NullPointerException("pat is null");
        }
        if (this.parent != null) {
            this.path = this.parent.path + path;
        }
        else {
            this.path = path;
        }
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

    public Route serviceIndex(String serviceIndex) {
        if (serviceIndex != null) {
            this.serviceIndex = Integer.valueOf(serviceIndex);
        }
        else if (this.parent != null && this.parent.serviceIndex != null) {
            this.serviceIndex = this.parent.serviceIndex;
        }
        else {
            this.serviceIndex = -1;
        }
        return this;
    }

    public Route method(String method) {
        if (StringUtils.isNotBlank(method)) {
            this.method = method;
        }
        else if (parent != null) {
            // 子路由method不能为空
            throw new NullPointerException("method is null for " + this.path);
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
            this.actions = List.of(HttpMethod.GET, HttpMethod.POST);
        }
        return this;
    }

    public Route filters(String filters) {
        if (StringUtils.isNotBlank(filters)) {
            this.filters = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(filters);
        }
        return this;
    }

}
