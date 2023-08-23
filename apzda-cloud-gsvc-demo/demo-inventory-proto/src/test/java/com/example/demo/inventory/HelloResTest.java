package com.example.demo.inventory;

import com.apzda.cloud.gsvc.proto.HelloRes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HelloResTest {
    @Test
    void the_default_value_is_ok() {
        HelloRes defaultInstance = HelloRes.newBuilder().build();

        assertThat(defaultInstance.getErrCode()).isEqualTo(0);
        assertThat(defaultInstance.getErrMsg()).isEmpty();
    }
}
