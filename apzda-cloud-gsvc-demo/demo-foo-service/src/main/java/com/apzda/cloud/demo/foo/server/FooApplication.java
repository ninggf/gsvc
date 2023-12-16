package com.apzda.cloud.demo.foo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author fengz
 */
@SpringBootApplication
@EnableFooServer
public class FooApplication {

    public static void main(String[] args) {
        SpringApplication.run(FooApplication.class, args);
    }

}
