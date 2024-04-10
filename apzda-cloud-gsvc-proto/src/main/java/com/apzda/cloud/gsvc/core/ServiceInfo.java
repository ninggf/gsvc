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

    /**
     * Mico Service name
     */
    String cfgName;

    String serviceName;

    String appName;

    Class<?> clazz;

    boolean local;

    String type;
    static final ServiceInfo DEFAULT = ServiceInfo.builder().local(true).build();

}
