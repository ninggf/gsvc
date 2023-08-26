package com.example.allinone.app;

import com.apzda.cloud.gsvc.autoconfigure.AllInOneApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author fengz
 */
@AllInOneApplication(appPackages = { "com.example.inventory", "com.example.order" })
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
