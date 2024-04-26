package com.apzda.cloud.sentinel.callback;

import cn.hutool.core.lang.UUID;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.apzda.cloud.adapter.spring.callback.BlockExceptionHandler;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * @author fengz
 */
@Slf4j
public class StandardUrlBlockHandler implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException ex) throws IOException {
        val context = GsvcContextHolder.getContext();
        val requestId = StringUtils.defaultIfBlank(context.getRequestId(), UUID.randomUUID().toString(true));
        context.setRequestId(requestId);

        if (log.isDebugEnabled()) {
            val caller = context.getCaller();
            log.debug("Visit {} from {} is blocked by Sentinel", request.getRequestURI(), caller);
        }
        // TODO 允许跳转到block页
        val fallback = ResponseUtils.fallback(ServiceError.TOO_MANY_REQUESTS, "", String.class);
        response.setHeader("X-Request-Id", requestId);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + "; charset=" + StandardCharsets.UTF_8);
        response.setContentLength(fallback.length());

        try (PrintWriter out = response.getWriter()) {
            out.print(fallback);
            out.flush();
        }
    }

}
