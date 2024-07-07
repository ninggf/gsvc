package com.apzda.cloud.demo.allinone;

import com.apzda.cloud.demo.bar.server.EnableBarServer;
import com.apzda.cloud.demo.foo.server.EnableFooServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author fengz
 */
@SpringBootApplication
@EnableBarServer
@EnableFooServer
public class AllInOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllInOneApplication.class, args);
    }

}
