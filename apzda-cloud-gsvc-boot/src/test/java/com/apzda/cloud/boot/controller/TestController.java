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
package com.apzda.cloud.boot.controller;

import com.apzda.cloud.boot.dict.Dictionary;
import com.apzda.cloud.boot.entity.User;
import com.apzda.cloud.boot.enums.TestStatus;
import com.apzda.cloud.boot.enums.TestStatus2;
import com.apzda.cloud.boot.enums.TestStatus3;
import com.apzda.cloud.boot.service.IUserService;
import com.apzda.cloud.boot.vo.TestVo;
import com.apzda.cloud.gsvc.dto.Response;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/user")
public class TestController extends CrudController<String, User, IUserService> {

    @Autowired
    private IUserService userService;

    @Dictionary
    @GetMapping("/test/{id}")
    public Response<TestVo> testVoResponse(@PathVariable String id) {
        TestVo testVo = new TestVo();
        testVo.setName("test = " + id);
        testVo.setStatus(TestStatus.T1);
        testVo.setStatus2(TestStatus2.T2);
        testVo.setStatus3(TestStatus3.T3);
        testVo.setPhone("13166666666");
        testVo.setPhone1("13166666666");
        return Response.success(testVo);
    }

    @Dictionary
    public Response<List<User>> getUserList() {
        return Response.success(userService.list());
    }

    @Dictionary
    public Response<IPage<User>> getUserPage() {
        IPage<User> page = new Page<>();
        page.setSize(100L);
        page.setCurrent(1L);
        return Response.success(userService.page(page));
    }

}
