package com.apzda.cloud.gsvc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class Response implements Serializable {

    @Serial
    private static final long serialVersionUID = 2865776613817344868L;

    private int errCode;

    private String errMsg;

    public static Response error(int code, String errMsg) {
        val resp = new Response();
        resp.errCode = code;
        resp.errMsg = errMsg;
        return resp;
    }

    public static Response error(int code) {
        return error(code, null);
    }

}
