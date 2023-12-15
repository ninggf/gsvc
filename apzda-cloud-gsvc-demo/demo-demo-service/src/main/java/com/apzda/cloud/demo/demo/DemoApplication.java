package com.apzda.cloud.demo.demo;

import com.apzda.cloud.demo.bar.proto.BarServiceGsvc;
import com.apzda.cloud.demo.bar.proto.FileServiceGsvc;
import com.apzda.cloud.demo.foo.proto.FooServiceGsvc;
import com.apzda.cloud.demo.math.proto.MathServiceGsvc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * @author fengz
 */
@SpringBootApplication
@Import({ FooServiceGsvc.class, BarServiceGsvc.class, MathServiceGsvc.class, FileServiceGsvc.class })
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
