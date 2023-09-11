package com.apzda.cloud.gsvc.plugin;

import org.springframework.core.Ordered;

/**
 * @author fengz
 */
public interface IGlobalPlugin extends IPlugin, Ordered {

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
