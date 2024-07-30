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
package com.apzda.cloud.demo.math.service;

import com.apzda.cloud.demo.math.domain.mapper.AccountMapper;
import com.apzda.cloud.demo.math.proto.AccountService;
import com.apzda.cloud.demo.math.proto.DebitDto;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;

    @Override
    @Transactional
    public GsvcExt.CommonRes debit(DebitDto request) {
        if (accountMapper.debitByUserId(request.getUserId(), request.getMoney()) > 0) {
            return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
        }
        return GsvcExt.CommonRes.newBuilder().setErrCode(999999).build();

    }

}
