package com.apzda.cloud.gsvc.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * @author fengz
 */
public class GsvcServletFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        val requestId = StringUtils.defaultIfBlank(request.getHeader("X-Request-ID"), UUID.randomUUID().toString());
        request.setAttribute("X-Request-ID", requestId);
        response.setHeader("X-Request-ID", requestId);

        filterChain.doFilter(request, response);
    }
}
