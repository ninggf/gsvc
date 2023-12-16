package com.apzda.cloud.demo.bar.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @author fengz
 */
@SpringBootApplication
@EnableBarServer
@EnableMethodSecurity
public class BarApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarApplication.class, args);
    }

}
