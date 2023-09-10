/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.core;

import lombok.Builder;
import lombok.Getter;

/**
 * @author fengz
 */
@Getter
@Builder
public class ServiceInfo {

    int index;

    String appName;

    String serviceName;

    String contextPath;

    String shortName;

    Class<?> clazz;

    boolean local;

    static final ServiceInfo DEFAULT = ServiceInfo.builder().build();

}
