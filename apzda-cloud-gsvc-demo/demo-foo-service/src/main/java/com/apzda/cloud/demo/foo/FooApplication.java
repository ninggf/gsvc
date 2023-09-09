package com.apzda.cloud.demo.foo;

import com.apzda.cloud.demo.bar.proto.BarServiceGsvc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * @author fengz
 */
@SpringBootApplication
@Import(BarServiceGsvc.class)
@PropertySource("classpath:foo.service.properties")
public class FooApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder().sources(FooApplication.class).run(args);
    }

}
