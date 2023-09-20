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
import com.apzda.cloud.gsvc.dto.Response;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public Response<BarRes> barHi(@RequestParam String name, @RequestParam int age) {
        val req = BarReq.newBuilder().setName(name).setAge(age).build();
        val res = barService.hi(req).blockFirst();

        return Response.wrap(res);
    }

    @GetMapping("/foo/hi")
    public Response<FooRes> fooHi(@RequestParam String name, @RequestParam int age) {
        val req = FooReq.newBuilder().setName(name).setAge(age).build();
        val res = fooService.hi(req);
        return Response.wrap(res.blockFirst());
    }

    @GetMapping("/greeting")
    public Response<DemoRes> greeting(@RequestParam String name) {
        return Response.wrap(demoService.greeting(DemoReq.newBuilder().setName(name).buildPartial()));
    }

}
