package com.example.inventory.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz
 */
@RestController
public class DemoController {

    @GetMapping("/demo/h")
    public String hello() {
        StpUtil.checkLogin();
        return "hello";
    }

}
