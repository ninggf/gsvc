/*
 * This file is part of apzda created at 2023/7/7 by ningGf.
 */
package com.apzda.module.test.abc.def.a.mapper;

import com.apzda.boot.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Created at 2023/7/7 13:06.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@Mapper
public interface UserMapper extends BaseMapper<User> {

    User getUserById(String id);

}
