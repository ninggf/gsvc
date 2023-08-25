package com.apzda.cloud.gsvc.filter;

import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * @author fengz
 */
@Order(SaTokenConsts.ASSEMBLY_ORDER + 1)
public class GsvcFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest request) {
            val header = request.getHeader("X-Request-Id");
            if (!StringUtils.hasText(header)) {
                val rid = UUID.randomUUID().toString();
                request.setAttribute("X-Request-Id", rid);
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
