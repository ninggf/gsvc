package com.apzda.cloud.gsvc.error;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz
 */
@RequiredArgsConstructor
public class GsvcErrorAttributes extends DefaultErrorAttributes {

    private final GsvcExceptionHandler handler;

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        // super.getErrorAttributes(webRequest, options);
        Map<String, Object> errorAttributes = new HashMap<>();
        Throwable error = getError(webRequest);

        val status = getStatus(webRequest);

        Response<?> response = Response.error(ServiceError.valueOf(status));
        if (error != null) {
            while (error instanceof ServletException && error.getCause() != null) {
                error = error.getCause();
            }
            response = handler.handle(error, true);
        }
        errorAttributes.put("errCode", response.getErrCode());
        errorAttributes.put("errMsg", response.getErrMsg());
        return errorAttributes;
    }

    protected HttpStatus getStatus(WebRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE,
                RequestAttributes.SCOPE_REQUEST);
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        }
        catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

}
