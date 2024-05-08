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
package com.apzda.cloud.demo.bar.controller;

import com.apzda.cloud.demo.math.proto.MathService;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/bar/")
@RequiredArgsConstructor
public class BarController {

    private final MathService mathService;

    @GetMapping("/acl/hello")
    public String hello(String name) {
        return name;
    }

    @GetMapping("/acl/hi")
    public String aclHi(String name) {
        return name;
    }

    @GetMapping("/method/greeting")
    @PreAuthorize("isAuthenticated()")
    public String greeting(String name) {
        return name;
    }

    @GetMapping("/method/hi")
    @PreAuthorize("hasRole('sa')")
    public String hi(String name) {
        return name;
    }

    @GetMapping("/rpc/hi")
    public String rpcHi(String name) {
        mathService.authed(Empty.newBuilder().build());
        return name;
    }

    @GetMapping("/rpc/deny")
    public String rpcDeny(String name) {
        mathService.deny(Empty.newBuilder().build());
        return name;
    }

}
