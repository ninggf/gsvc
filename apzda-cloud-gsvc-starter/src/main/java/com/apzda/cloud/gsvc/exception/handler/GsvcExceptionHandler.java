package com.apzda.cloud.gsvc.exception.handler;


import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.apzda.cloud.gsvc.ServiceError;
import com.apzda.cloud.gsvc.core.SaTokenExtendProperties;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.exception.GsvcException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author ninggf
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GsvcExceptionHandler {
    private final SaTokenExtendProperties properties;

    public ServerResponse handle(ServerRequest request, Throwable e) {
        if (e instanceof NotLoginException notLoginException) {
            val code = notLoginException.getCode();
            val contentTypes = request.headers().accept();
            val loginUrl = properties.getLoginUrl();
            val textType = MediaType.parseMediaType("text/*");

            if (loginUrl != null && !CollectionUtils.isEmpty(contentTypes)) {
                for (MediaType contentType : contentTypes) {
                    if (contentType.isCompatibleWith(textType)) {
                        // redirect to the login page
                        return ServerResponse.status(HttpStatus.TEMPORARY_REDIRECT).location(loginUrl).build();
                    }
                }
            }
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(handle(e));
    }

    @ResponseBody
    @ExceptionHandler(Throwable.class)
    public Response handle(Throwable e) {
        if (e instanceof NotLoginException notLoginException) {
            int errCode = 10700 - notLoginException.getCode();
            val response = new Response();
            response.setErrCode(errCode);
            response.setErrMsg(notLoginException.getMessage());
            return response;
        } else if (e instanceof NotPermissionException) {
            val response = new Response();
            response.setErrCode(ServiceError.REMOTE_SERVICE_FORBIDDEN.code);
            response.setErrMsg(ServiceError.REMOTE_SERVICE_FORBIDDEN.message);
            return response;
        } else if (e instanceof GsvcException gsvcException) {
            val response = new Response();
            val error = gsvcException.getError();
            response.setErrMsg(error.message);
            response.setErrCode(error.code);
            return response;
        } else {
            val response = new Response();
            val error = ServiceError.SERVICE_ERROR;
            response.setErrMsg(error.message);
            response.setErrCode(error.code);
            return response;
        }
    }
}
