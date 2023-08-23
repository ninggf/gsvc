package com.demo.webfux.controller;

import com.demo.webfux.model.User;
import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("strings")
    public Flux<String> strings() {
        return Flux.just("hello", "world", "!");
    }

    @GetMapping("/users")
    public Flux<User> users() {
        return Flux.create((fluxSink -> {
            CompletableFuture.runAsync(() -> {
                val user = new User();
                fluxSink.next(user.setAge(18).setName("小伙子"));
                try {
                    TimeUnit.SECONDS.sleep(3);
                    fluxSink.next(user.setAge(28).setName("中伙子"));
                    TimeUnit.SECONDS.sleep(3);
                    fluxSink.next(user.setAge(38).setName("老伙子"));
                    fluxSink.complete();
                } catch (InterruptedException e) {
                    fluxSink.error(e);
                }
            });
        }));
    }
}
