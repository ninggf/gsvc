package com.example.inventory;

import lombok.val;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author fengz
 */
@SpringBootApplication
public class InventoryDemoApplication {

    public static void main(String[] args) {
        val appBuilder = new SpringApplicationBuilder();
        val app = appBuilder.sources(InventoryDemoApplication.class).build(args);
        app.run(args);
    }

}
