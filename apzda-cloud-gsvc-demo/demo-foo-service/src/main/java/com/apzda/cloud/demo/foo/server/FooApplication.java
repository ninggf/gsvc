package com.apzda.cloud.demo.foo.server;

import com.apzda.cloud.demo.bar.proto.BarApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author fengz
 */
@SpringBootApplication
@EnableFooServer
@EnableFeignClients(basePackageClasses = { BarApi.class })
public class FooApplication {

    public static void main(String[] args) {
        SpringApplication.run(FooApplication.class, args);
    }

}
