package com.apzda.cloud.gsvc.dto;

import com.apzda.cloud.gsvc.IServiceError;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz windywany@gmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 2865776613817344868L;

    private int errCode;

    private String errMsg;

    private String message;

    private MessageType type;

    private T data;

    public Response<?> type(MessageType type) {
        this.type = type;
        return this;
    }

    public static Response<Void> error(IServiceError error) {
        return error(error.code(), error.message());
    }

    public static Response<Void> error(int code, String errMsg) {
        val resp = new Response<Void>();
        resp.errCode = code;
        resp.errMsg = errMsg;
        return resp;
    }

    public static Response<Void> error(int code) {
        return error(code, null);
    }

    public static <T> Response<T> success(T data, String message) {
        val resp = new Response<T>();
        resp.setData(data);
        resp.setMessage(message);
        return resp;
    }

    public static <T> Response<T> success(T data) {
        return success(data, null);
    }

    public static Response<Void> success(String message) {
        return success(null, message);
    }

}
