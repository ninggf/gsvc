package com.apzda.cloud.demo.allinone;

import com.apzda.cloud.demo.bar.server.EnableBarServer;
import com.apzda.cloud.demo.foo.server.EnableFooServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author fengz
 */
@SpringBootApplication(
        scanBasePackages = { "com.apzda.cloud.demo.allinone.controller", "com.apzda.cloud.demo.math.service" })
@EnableBarServer
@EnableFooServer
@MapperScan("com.apzda.cloud.demo.math.domain.mapper")
public class AllInOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllInOneApplication.class, args);
    }

}
