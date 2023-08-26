package com.example.gtw;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @author fengz
 */
@SpringBootApplication
@EnableEurekaServer
public class GtwDemoApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GtwDemoApplication.class).web(WebApplicationType.SERVLET).run(args);
    }

}
