package com.apzda.cloud.gsvc.error;

import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.val;
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
        return super.getErrorAttributes(webRequest, options);
    }
}
