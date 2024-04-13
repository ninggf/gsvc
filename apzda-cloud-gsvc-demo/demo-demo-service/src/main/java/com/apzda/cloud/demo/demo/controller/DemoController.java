/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.demo.demo.controller;

import com.apzda.cloud.demo.bar.proto.*;
import com.apzda.cloud.demo.demo.proto.DemoReq;
import com.apzda.cloud.demo.demo.proto.DemoRes;
import com.apzda.cloud.demo.demo.proto.DemoService;
import com.apzda.cloud.demo.foo.proto.FooReq;
import com.apzda.cloud.demo.foo.proto.FooRes;
import com.apzda.cloud.demo.foo.proto.FooService;
import com.apzda.cloud.demo.math.proto.MathService;
import com.apzda.cloud.demo.math.proto.OpNum;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.ext.FileUploadUtils;
import com.google.common.base.Joiner;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

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

    private final MathService mathService;

    private final FileService fileService;

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

    @GetMapping("/add/{n1}/{n2}")
    public Response<Long> add(@PathVariable Integer n1, @PathVariable Integer n2) {
        val result = mathService.add(OpNum.newBuilder().setNum1(n1).setNum2(n2).build());
        return Response.success(result.getResult());
    }

    @GetMapping("/sum")
    public Response<Long> sum() {
        val sum = mathService
            .sum(Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).map(integer -> OpNum.newBuilder().setNum1(integer).build()));
        val result = sum.blockFirst();
        return Response.success(result.getResult());
    }

    @GetMapping("/even/{n1}/{n2}")
    public Response<String> even(@PathVariable Integer n1, @PathVariable Integer n2) {
        val result = mathService.even(OpNum.newBuilder().setNum1(n1).setNum2(n2).build());
        val nums = new ArrayList<String>();
        result.doOnNext(n -> {
            nums.add(String.valueOf(n.getResult()));
        }).blockLast();

        return Response.success(Joiner.on(",").join(nums), "even");
    }

    @GetMapping("/square")
    public Response<String> square() {
        val squares = mathService.square(Flux.just(1, 2, 3).map(n -> OpNum.newBuilder().setNum1(n).build()));

        val nums = new ArrayList<String>();

        squares.doOnNext(n -> {
            nums.add(String.valueOf(n.getResult()));
        }).blockLast();

        return Response.success(Joiner.on(",").join(nums), "square");
    }

    @GetMapping("/upload")
    public Response<UploadRes> upload() throws IOException {
        val builder = FileReq.newBuilder();
        builder.setPath("abc");

        builder.addNames("a");
        builder.addNames("b");

        val file = new File("./index.md");
        FileCopyUtils.copy("# INDEX", new FileWriter(file));
        val f1 = FileUploadUtils.create(file);
        builder.addFiles(f1);

        val file2 = new File("./index.xml");
        FileCopyUtils.copy("<root>test</root>", new FileWriter(file2));
        val f2 = FileUploadUtils.create(file2);
        builder.addFiles(f2);

        val file3 = new File("./index.txt");
        FileCopyUtils.copy("<root>test</root>", new FileWriter(file3));
        val f3 = FileUploadUtils.create(file3);
        builder.setFile(f3);

        val upload = fileService.upload(builder.build());

        file.delete();
        file2.delete();
        file3.delete();

        return Response.success(upload);
    }

    @PostMapping("/upload")
    public Response<String> uploadLocal(MultipartFile file) throws IOException {
        return Response.ok(file.getOriginalFilename());
    }

}
