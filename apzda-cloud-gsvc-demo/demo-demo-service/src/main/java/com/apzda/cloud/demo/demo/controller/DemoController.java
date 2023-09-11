/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.demo.demo.controller;

import com.apzda.cloud.demo.bar.proto.BarReq;
import com.apzda.cloud.demo.bar.proto.BarRes;
import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.demo.proto.DemoReq;
import com.apzda.cloud.demo.demo.proto.DemoRes;
import com.apzda.cloud.demo.demo.proto.DemoService;
import com.apzda.cloud.demo.foo.proto.FooReq;
import com.apzda.cloud.demo.foo.proto.FooRes;
import com.apzda.cloud.demo.foo.proto.FooService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Created at 2023/9/10 15:49.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequiredArgsConstructor
public class DemoController {

    private final BarService barService;

    private final FooService fooService;

    private final DemoService demoService;

    @GetMapping("/bar/hi")
    public BarRes barHi(@RequestParam String name, @RequestParam int age) {
        val req = BarReq.newBuilder().setName(name).setAge(age).build();
        val res = barService.hi(Mono.just(req));

        return res.block();
    }

    @GetMapping("/foo/hi")
    public FooRes fooHi(@RequestParam String name, @RequestParam int age) {
        val req = FooReq.newBuilder().setName(name).setAge(age).build();
        val res = fooService.hi(Mono.just(req));
        return res.block();
    }

    @GetMapping("/greeting")
    public DemoRes greeting(@RequestParam String name) {
        return demoService.greeting(DemoReq.newBuilder().setName(name).buildPartial());
    }

}
