package com.apzda.cloud.gsvc.gtw;

import com.google.common.base.Splitter;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author fengz
 */
@Data
public class Route {

    private String path;

    private Boolean login;

    private String access;

    private String method;

    private String summary;

    private String desc;

    private String[] tags;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration readTimeout = Duration.ZERO;

    private List<HttpMethod> actions = Collections.emptyList();

    private Set<String> filters = new HashSet<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Route parent;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    int index;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    String prefix;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    String contextPath = "";

    public int index() {
        if (parent != null) {
            return (parent.index() + 1) * 100000 + this.index;
        }
        return this.index;
    }

    public Route contextPath(String contextPath) {
        this.contextPath = StringUtils.stripEnd(StringUtils.defaultIfBlank(contextPath, ""), "/");
        return this;
    }

    public String contextPath() {
        return this.contextPath;
    }

    public Route prefix(String prefix) {
        this.prefix = prefix;
        return this;
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

    public Route access(String access) {
        if (access != null) {
            this.access = access;
        }
        else if (this.parent != null && this.parent.access != null) {
            this.access = this.parent.access;
        }
        if (StringUtils.isNotBlank(this.access)) {
            this.login = true;
        }
        return this;
    }

    public Route method(String method) {
        if (StringUtils.isNotBlank(method)) {
            this.method = method;
        }
        else if (this.parent != null) {
            // 子路由method不能为空
            throw new IllegalArgumentException(String.format("Method of %s.routes[%d].routes[%d] is blank", this.prefix,
                    this.parent.index, index));
        }
        return this;
    }

    public Route actions(String actions) {
        if ("*".equals(actions)) {
            this.actions = Collections.emptyList();
        }
        else if (StringUtils.isNotBlank(actions)) {
            this.actions = Splitter.on(",")
                .omitEmptyStrings()
                .trimResults()
                .splitToList(actions)
                .stream()
                .map(action -> HttpMethod.valueOf(action.toUpperCase()))
                .toList();
            if (CollectionUtils.isEmpty(this.actions)) {
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

        if (parent != null && !CollectionUtils.isEmpty(parent.filters)) {
            val mergedFilters = new HashSet<String>(parent.filters);
            mergedFilters.addAll(this.filters);
            this.filters = mergedFilters;
        }

        return this;
    }

    public Route readTimeout(Duration readTimeout) {
        if (readTimeout.toMillis() > 0) {
            this.readTimeout = readTimeout;
        }
        else if (parent != null) {
            this.readTimeout = parent.readTimeout;
        }
        else {
            this.readTimeout = Duration.ZERO;
        }
        return this;
    }

    public Route summary(String summary) {
        this.summary = StringUtils.defaultString(summary);
        return this;
    }

    public Route desc(String desc) {
        this.desc = StringUtils.defaultString(desc);
        return this;
    }

    public Route tags(String tags) {
        if (StringUtils.isNotBlank(tags)) {
            val tagList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(tags);
            this.tags = new String[tagList.size()];

            for (int i = 0; i < this.tags.length; i++) {
                this.tags[i] = tagList.get(i);
            }
        }
        else if (this.parent != null) {
            this.tags = this.parent.tags;
        }
        else {
            this.tags = new String[] {};
        }
        return this;
    }

    public String absPath() {
        if (parent != null) {
            return contextPath + parent.getPath() + path;
        }
        return contextPath + path;
    }

    public RouteMeta meta() {
        return new RouteMeta().setLogin(login).setAccess(this.access);
    }

    @Override
    public String toString() {
        val str = new ToStringCreator(this);

        str.append("path", absPath())
            .append("method", method)
            .append("timeout", readTimeout)
            .append("index", index())
            .append("login", login)
            .append("access", access)
            .append("actions", actions)
            .append("filters", filters);

        return str.toString();
    }

}
