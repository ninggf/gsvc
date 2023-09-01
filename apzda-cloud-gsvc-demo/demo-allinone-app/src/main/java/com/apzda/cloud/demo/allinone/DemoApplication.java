package com.apzda.cloud.demo.allinone;

import com.apzda.cloud.gsvc.autoconfigure.AllInOneApplication;
import org.springframework.boot.SpringApplication;

/**
 * @author fengz
 */
@AllInOneApplication(appPackages = { "com.apzda.cloud.demo.bar"})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
