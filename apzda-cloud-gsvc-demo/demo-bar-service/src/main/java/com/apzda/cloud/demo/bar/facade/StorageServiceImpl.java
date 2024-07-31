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
package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.domain.mapper.StorageMapper;
import com.apzda.cloud.demo.bar.proto.DeductDto;
import com.apzda.cloud.demo.bar.proto.StorageResp;
import com.apzda.cloud.demo.bar.proto.StorageService;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.protobuf.Empty;
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
public class StorageServiceImpl implements StorageService {

    private final StorageMapper storageMapper;

    @Override
    @Transactional
    public GsvcExt.CommonRes deduct(DeductDto request) {
        val count = request.getCount();
        val commodityCode = request.getCommodityCode();
        if (storageMapper.deductByCommodityCode(commodityCode, count) > 0) {
            return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
        }
        return GsvcExt.CommonRes.newBuilder().setErrCode(999999).build();
    }

    @Override
    @Transactional
    public GsvcExt.CommonRes reset(Empty request) {
        if (storageMapper.reset() > 0) {
            return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
        }
        return GsvcExt.CommonRes.newBuilder().setErrCode(999999).build();
    }

    @Override
    public StorageResp query(Empty request) {
        val storages = storageMapper.selectList(Wrappers.query());
        val resp = StorageResp.newBuilder();
        for (val storage : storages) {
            resp.addStorage(DeductDto.newBuilder()
                .setCount(storage.getCount())
                .setCommodityCode(storage.getCommodityCode())
                .build());
        }
        return resp.build();
    }

}
