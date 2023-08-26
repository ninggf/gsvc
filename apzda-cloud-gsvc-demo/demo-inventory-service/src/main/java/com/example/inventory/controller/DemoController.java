package com.example.inventory.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author fengz
 */
@Controller
public class DemoController {

    @ResponseBody
    @GetMapping(value = "/demo/h", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hello() {
        StpUtil.checkLogin();
        return "hello";
    }

    @GetMapping("/demo/hx")
    public String hellox() {
        StpUtil.checkLogin();
        return "hello";
    }

}
