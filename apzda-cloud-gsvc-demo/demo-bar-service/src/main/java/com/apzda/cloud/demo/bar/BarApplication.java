package com.apzda.cloud.demo.bar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

/**
 * @author fengz
 */
@SpringBootApplication
@PropertySource("classpath:bar.service.properties")
public class BarApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarApplication.class, args);
    }

}