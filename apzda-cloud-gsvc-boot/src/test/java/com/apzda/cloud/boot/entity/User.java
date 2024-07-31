/*
 * This file is part of apzda created at 2023/7/7 by ningGf.
 */
package com.apzda.cloud.boot.entity;

import com.apzda.cloud.boot.dict.Dict;
import com.apzda.cloud.boot.transformer.Upper;
import com.apzda.cloud.gsvc.acl.Resource;
import com.apzda.cloud.gsvc.model.IEntity;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Created at 2023/7/7 13:18.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@TableName(value = "t_users", autoResultMap = true)
@Resource
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User implements IEntity<String> {

    @TableId(type = IdType.ASSIGN_ID, value = "uid")
    private String id;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String merchantId;

    @Dict(transformer = Upper.class)
    private String name;

    @Version
    private Long ver;

    @TableLogic
    private Integer del;

    @Dict(entity = Role.class, code = "rid", value = "name")
    private String roles;

    @Dict(code = "test")
    private String type;

}
