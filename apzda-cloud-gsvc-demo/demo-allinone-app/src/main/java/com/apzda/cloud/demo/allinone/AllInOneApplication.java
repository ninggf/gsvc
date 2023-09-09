package com.apzda.cloud.demo.allinone;

import com.apzda.cloud.demo.bar.BarApplication;
import com.apzda.cloud.demo.foo.FooApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author fengz
 */
@SpringBootApplication
public class AllInOneApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext app = new SpringApplicationBuilder().sources(AllInOneApplication.class)
            .sources(BarApplication.class)
            .sources(FooApplication.class)
            .run(args);
    }

}
