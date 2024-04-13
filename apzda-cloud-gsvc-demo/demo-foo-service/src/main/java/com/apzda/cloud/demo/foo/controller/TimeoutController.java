/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.demo.foo.controller;

import com.apzda.cloud.demo.foo.proto.FooService;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/timeout")
@RequiredArgsConstructor
@Slf4j
public class TimeoutController {

    private final FooService fooService;

    @GetMapping("/sleep1")
    public String sleep1() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        log.trace("Sleep1 complete");
        return "ok";
    }

    @GetMapping("/sleep2")
    public String sleep2() throws InterruptedException {
        fooService.sleep2(Empty.newBuilder().build());
        return "ok";
    }

    @GetMapping("/sleep3")
    public String sleep3() throws InterruptedException {
        fooService.sleep3(Empty.newBuilder().build()).blockLast();
        return "ok";
    }

}
