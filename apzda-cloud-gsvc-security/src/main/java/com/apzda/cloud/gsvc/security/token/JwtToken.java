/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz windywany@gmail.com
 **/
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtToken implements Serializable {

    @Serial
    private static final long serialVersionUID = -2763131228048354173L;

    private String name;

    private String accessToken;

    private String refreshToken;

}
