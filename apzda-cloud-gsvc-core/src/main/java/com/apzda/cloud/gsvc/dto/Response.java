package com.apzda.cloud.gsvc.dto;

import com.apzda.cloud.gsvc.IServiceError;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz windywany@gmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response")
public class Response<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 2865776613817344868L;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "0 means success, others value mean error.")
    private int errCode;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "The detail message of the error.")
    private String errMsg;

    @Schema(description = "alias of errMsg")
    private String message;

    @Schema(description = "When the errMsg(message) is not blank, client show the errMsg by 'type' method")
    private MessageType type;

    @Schema(description = "The business data")
    private T data;

    public Response<T> type(MessageType type) {
        this.type = type;
        return this;
    }

    public Response<T> alert(String message) {
        this.type = MessageType.ALERT;
        this.errMsg = message;
        return this;
    }

    public Response<T> notify(String message) {
        this.type = MessageType.NOTIFY;
        this.errMsg = message;
        return this;
    }

    public Response<T> toast(String message) {
        this.type = MessageType.TOAST;
        this.errMsg = message;
        return this;
    }

    public Response<T> none(String message) {
        this.type = MessageType.NONE;
        this.errMsg = message;
        return this;
    }

    public static <T> Response<T> wrap(T data) {
        Assert.notNull(data, "data is null!");
        val resp = new Response<T>();
        BeanUtils.copyProperties(data, resp, "data", "type", "message");
        resp.setData(data);
        return resp;
    }

    public static <T> Response<T> error(IServiceError error) {
        Response<T> resp = error(error.code(), error.message());
        resp.type = error.type();
        return resp;
    }

    public static <T> Response<T> error(int code, String errMsg) {
        val resp = new Response<T>();
        resp.errCode = code;
        resp.errMsg = errMsg;
        return resp;
    }

    public static <T> Response<T> error(int code) {
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

    public static <T> Response<T> success(String message) {
        return success(null, message);
    }

}
