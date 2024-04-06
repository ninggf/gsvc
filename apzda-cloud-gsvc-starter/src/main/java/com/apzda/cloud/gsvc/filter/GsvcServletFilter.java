package com.apzda.cloud.gsvc.filter;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.UUID;

/**
 * @author fengz
 */
@RequiredArgsConstructor
public class GsvcServletFilter extends OncePerRequestFilter {

    private final LocaleResolver localeResolver;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        val requestId = StringUtils.defaultIfBlank(request.getHeader("X-Request-ID"), UUID.randomUUID().toString());
        request.setAttribute("X-Request-ID", requestId);
        response.setHeader("X-Request-ID", requestId);
        // bookmark: 初始化
        val context = GsvcContextHolder.current();
        context.setRequestId(requestId);
        try {
            context.setLocale(localeResolver.resolveLocale(request));
        }
        catch (Exception ignored) {
        }
        try {
            filterChain.doFilter(request, response);
        }
        finally {
            GsvcContextHolder.clear();
        }
    }

}
