package com.apzda.cloud.gsvc.dto;

import com.apzda.cloud.gsvc.security.IUser;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz
 */
@Data
public class CurrentUser implements IUser, Serializable {

    @Serial
    private static final long serialVersionUID = -2525630989963990423L;

    private String uid;

    private String name;

}
