package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.proto.FooBarService;
import com.apzda.cloud.gsvc.proto.GreetingService;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created at 2023/8/19 14:29.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
class GatewayServiceRegistryTest {

    @org.junit.jupiter.api.Test
    void shortSvcName() {
        // given
        Class<?> clazz = FooBarService.class;

        // when
        val cfgName = GatewayServiceRegistry.cfgName(clazz);
        // then
        assertThat(cfgName).isEqualTo("FooBarService");
    }

    @org.junit.jupiter.api.Test
    void svcName() {
        // given
        Class<?> clazz = GreetingService.class;

        // when
        val svcName = GatewayServiceRegistry.svcName(clazz);
        // then
        assertThat(svcName).isEqualTo("greetingService");
    }

}
