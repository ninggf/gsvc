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
package com.apzda.cloud.demo.foo.service;

import com.apzda.cloud.demo.foo.domain.entity.Order;
import com.apzda.cloud.demo.foo.domain.mapper.OrderMapper;
import com.apzda.cloud.demo.foo.proto.CreateOrderDto;
import com.apzda.cloud.demo.foo.proto.OrderService;
import com.apzda.cloud.demo.math.proto.AccountService;
import com.apzda.cloud.demo.math.proto.DebitDto;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final AccountService accountService;

    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public GsvcExt.CommonRes create(CreateOrderDto request) {
        val order = new Order();
        order.setUserId(request.getUserId());
        order.setCommodityCode(request.getCommodityCode());
        order.setCount(request.getOrderCount());
        order.setMoney(request.getOrderAmount());

        if (orderMapper.insert(order) > 0) {
            val debit = DebitDto.newBuilder();
            debit.setUserId(order.getUserId());
            debit.setMoney(order.getMoney());
            val res = accountService.debit(debit.build());
            if (res.getErrCode() != 0) {
                throw new GsvcException(ServiceError.SERVICE_ERROR);
            }
            return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
        }
        else {
            return GsvcExt.CommonRes.newBuilder().setErrCode(999999).build();
        }
    }

}
