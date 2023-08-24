package com.example.inventory.config;

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;

import java.util.HashMap;


@Configuration
public class DemoConfig {
    @Bean
    RouterFunction<ServerResponse> myaasfa() {

        return RouterFunctions.route().GET("/demo/x", request -> {
            val stringStringHashMap = new HashMap<String, String>();
            stringStringHashMap.put("name", "3");
            val just = Flux.just(stringStringHashMap, stringStringHashMap, stringStringHashMap, stringStringHashMap);
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(just);
        }).build();
    }
}
