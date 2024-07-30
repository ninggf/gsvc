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

import com.apzda.cloud.demo.bar.dto.OrderDto;
import com.apzda.cloud.demo.bar.proto.DeductDto;
import com.apzda.cloud.demo.bar.proto.StorageService;
import com.apzda.cloud.demo.foo.proto.CreateOrderDto;
import com.apzda.cloud.demo.foo.proto.OrderService;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final StorageService storageService;

    private final OrderService orderService;

    @PostMapping("/create")
    @GlobalTransactional
    public Response<String> create(@RequestBody OrderDto orderDto) {
        // 减库存
        val deductDto = DeductDto.newBuilder().setCommodityCode(orderDto.getCommodityCode());
        deductDto.setCount(orderDto.getCount());

        val deduct = storageService.deduct(deductDto.build());
        if (deduct.getErrCode() != 0) {
            throw new GsvcException(ServiceError.SERVICE_ERROR);
        }
        // 创建订单
        val createOrderDto = CreateOrderDto.newBuilder();
        createOrderDto.setUserId(orderDto.getUserId());
        createOrderDto.setCommodityCode(orderDto.getCommodityCode());
        createOrderDto.setOrderCount(orderDto.getCount());
        createOrderDto.setOrderAmount(orderDto.getMoney());

        val order = orderService.create(createOrderDto.build());
        if (order.getErrCode() != 0) {
            throw new GsvcException(ServiceError.SERVICE_ERROR);
        }
        return Response.success("成功啦");
    }


}
