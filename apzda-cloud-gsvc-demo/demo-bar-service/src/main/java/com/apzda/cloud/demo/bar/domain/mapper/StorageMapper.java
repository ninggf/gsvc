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
package com.apzda.cloud.demo.bar.domain.mapper;

import com.apzda.cloud.demo.bar.domain.entity.Storage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Mapper
public interface StorageMapper extends BaseMapper<Storage> {

    @Update("UPDATE storage_tbl SET cnt = cnt - #{count} WHERE commodity_code = #{commodityCode}")
    int deductByCommodityCode(String commodityCode, int count);

    @Update("UPDATE storage_tbl SET cnt = 10 WHERE commodity_code = '123456'")
    int reset();

}
