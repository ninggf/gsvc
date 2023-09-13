/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz windywany@gmail.com
 **/
@Data
@Builder
public class JwtToken implements Serializable {

    @Serial
    private static final long serialVersionUID = -2763131228048354173L;

    private String uid;

    private String name;

    private String accessToken;

    private String refreshToken;

}
