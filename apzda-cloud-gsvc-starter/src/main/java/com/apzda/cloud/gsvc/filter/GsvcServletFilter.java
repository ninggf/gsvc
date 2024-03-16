package com.apzda.cloud.gsvc.filter;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;

import java.io.IOException;

/**
 * @author fengz
 */
public class GsvcServletFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {
            val requestId = GsvcContextHolder.getRequestId();
            ((HttpServletResponse) servletResponse).setHeader("X-Request-Id", requestId);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

}
