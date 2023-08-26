package com.example.inventory.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz
 */
@RestController
public class Demo2Controller {

    @GetMapping("/demo2/hx")
    public String hellox() {
        StpUtil.checkLogin();
        return "hello";
    }

}
