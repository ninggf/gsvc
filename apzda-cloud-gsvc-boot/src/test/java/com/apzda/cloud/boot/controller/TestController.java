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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/user")
public class TestController extends BaseCrudController<User, IUserService> {

    @Dictionary
    @GetMapping("/test/{id}")
    public Response<TestVo> testVoResponse(@PathVariable String id) {
        TestVo testVo = new TestVo();
        testVo.setName("test = " + id);
        testVo.setStatus(TestStatus.T1);
        testVo.setStatus2(TestStatus2.T2);
        testVo.setStatus3(TestStatus3.T3);
        return Response.success(testVo);
    }

    @GetMapping("/test1/{id}")
    public Response<TestVo> testVoResponse1(@PathVariable String id) {
        TestVo testVo = new TestVo();
        testVo.setName("test1 = " + id);
        return Response.success(testVo);
    }

}
