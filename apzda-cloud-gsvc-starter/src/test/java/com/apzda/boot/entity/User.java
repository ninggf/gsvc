/*
 * This file is part of apzda created at 2023/7/7 by ningGf.
 */
package com.apzda.boot.entity;

import com.apzda.mybatis.CommaFieldTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.List;

/**
 * Created at 2023/7/7 13:18.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@TableName(value = "t_users", autoResultMap = true)
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private String uid;
    private String name;
    @Version
    private Long ver;
    @TableLogic
    private Integer del;

    @TableField(typeHandler = CommaFieldTypeHandler.class)
    private List<String> roles;
}
