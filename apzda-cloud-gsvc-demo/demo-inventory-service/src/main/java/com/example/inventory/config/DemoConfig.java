package com.example.inventory.config;

import cn.dev33.satoken.stp.StpUtil;
import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;


@ControllerAdvice
@Configuration
public class DemoConfig {
    @Autowired
    GsvcExceptionHandler handler;

    @Bean
    RouterFunction<ServerResponse> myaasfa() {

        return RouterFunctions.route().GET("/demo/x", request -> {
            try {
                StpUtil.checkLogin();
                return ServerResponse.ok().body("hello");
            } catch (Exception e) {
                return handler.handle(request, e);
            }
        }).build();
    }
}
