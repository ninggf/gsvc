package com.apzda.cloud.sentinel.callback;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import com.apzda.cloud.gsvc.server.IServiceMethodHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import static com.apzda.cloud.gsvc.server.IServiceMethodHandler.GTW;

/**
 * @author fengz
 */
public class StandardRequestOriginParser implements RequestOriginParser {

    @Override
    public String parseOrigin(HttpServletRequest request) {
        return StringUtils.defaultIfBlank(request.getHeader(IServiceMethodHandler.CALLER_HEADER), GTW);
    }

}
