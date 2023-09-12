package com.apzda.cloud.gsvc.sentinel;

import com.apzda.cloud.adapter.servlet.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz
 */
public class DefaultRequestOriginParser implements RequestOriginParser {

    @Override
    public String parseOrigin(HttpServletRequest request) {
        return StringUtils.defaultIfBlank(request.getHeader("x-gsvc-caller"), "default");
    }

}
