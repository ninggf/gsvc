package com.apzda.cloud.gsvc.gtw;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
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
    private Class<?> interfaceName;
    private String method;
    private List<String> filters = new ArrayList<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Route parent;

    public Route parent(Route parent) {
        this.parent = parent;
        return this;
    }

    public Route path(String path) {
        return this;
    }

    public Route login(String login) {
        return this;
    }

    public Route interfaceName(String interfaceName) {
        return this;
    }

    public Route method(String method) {
        return this;
    }

    public Route filters(String filters) {
        return this;
    }
}
