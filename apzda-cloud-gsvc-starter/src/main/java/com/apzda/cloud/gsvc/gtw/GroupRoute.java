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

    private List<Route> routes = Collections.emptyList();

}
