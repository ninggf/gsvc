package com.apzda.cloud.demo.bar;

import com.apzda.cloud.demo.math.proto.MathServiceGsvc;
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @author fengz
 */
@SpringBootApplication
@PropertySource("classpath:bar.service.properties")
@Slf4j
@Import(MathServiceGsvc.class)
@EnableMethodSecurity
public class BarApplication {

    @Bean("bar.MessageSourceNameResolver")
    MessageSourceNameResolver messageSourceNameResolver() {
        return () -> "messages-bar";
    }

    public static void main(String[] args) {
        SpringApplication.run(BarApplication.class, args);
    }

}
