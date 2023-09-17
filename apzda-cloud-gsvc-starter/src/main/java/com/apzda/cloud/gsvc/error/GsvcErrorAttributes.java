package com.apzda.cloud.gsvc.error;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * @author fengz
 */
@RequiredArgsConstructor
public class GsvcErrorAttributes extends DefaultErrorAttributes {

    private final GsvcExceptionHandler handler;

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
        Throwable error = getError(webRequest);
        Response<?> response = Response.error(ServiceError.SERVICE_ERROR);
        if (error != null) {
            while (error instanceof ServletException && error.getCause() != null) {
                error = error.getCause();
            }
            response = handler.handle(error);
        }
        errorAttributes.put("errCode", response.getErrCode());
        errorAttributes.put("errMsg", response.getErrMsg());
        return errorAttributes;
    }

}
