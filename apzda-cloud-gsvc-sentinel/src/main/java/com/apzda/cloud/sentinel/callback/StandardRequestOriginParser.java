package com.apzda.cloud.sentinel.callback;

import com.apzda.cloud.adapter.spring.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz
 */
public class StandardRequestOriginParser implements RequestOriginParser {

    @Override
    public String parseOrigin(HttpServletRequest request) {
        return StringUtils.defaultIfBlank(request.getHeader("X-Gsvc-Caller"), "gtw");
    }

}
