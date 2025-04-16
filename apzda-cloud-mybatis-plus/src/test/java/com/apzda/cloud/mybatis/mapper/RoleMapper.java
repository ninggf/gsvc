/*
 * This file is part of apzda created at 2023/7/7 by ningGf.
 */
package com.apzda.cloud.mybatis.mapper;

import com.apzda.cloud.mybatis.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Created at 2023/7/7 15:16.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    Role getRoleById(String id);

}
