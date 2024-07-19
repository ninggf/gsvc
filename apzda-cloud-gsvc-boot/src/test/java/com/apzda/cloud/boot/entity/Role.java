/*
 * This file is part of apzda created at 2023/7/7 by ningGf.
 */
package com.apzda.cloud.boot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Created at 2023/7/7 15:15.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@TableName("t_roles")
public class Role {

    @TableId(type = IdType.ASSIGN_UUID)
    private String rid;

    private String name;

    @TableLogic
    private Integer del;

    private Double dd;

}
