package com.apzda.cloud.gsvc.core;

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

    @Test
    void should_be_a_unary_method() throws NoSuchMethodException {
        // given
        GatewayServiceRegistry.register("test", "greetingService", 0, null, GreetingService.class);

        // when
        val methods = GatewayServiceRegistry.getServiceMethods("test", "greetingService");

        // then
        assertThat(methods).isNotEmpty();
        assertThat(methods).containsKey("sayHello");
        assertThat(methods).containsKey("sayHei");
        assertThat(methods).containsKey("sayHi");
        System.out.println(methods);
    }

}
