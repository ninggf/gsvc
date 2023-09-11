package com.apzda.cloud.gsvc.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * @author fengz
 */
public class GsvcServletFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest request) {
            val requestId = StringUtils.defaultIfBlank(request.getHeader("X-Request-Id"), UUID.randomUUID().toString());
            request.setAttribute("X-Request-Id", requestId);
            ((HttpServletResponse) servletResponse).setHeader("X-Request-Id", requestId);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

}
