package com.apzda.cloud.demo.demo;

import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.bar.proto.FileService;
import com.apzda.cloud.demo.foo.proto.FooService;
import com.apzda.cloud.demo.math.proto.MathService;
import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author fengz
 */
@SpringBootApplication
@EnableGsvcServices({ FooService.class, BarService.class, MathService.class, FileService.class })
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
