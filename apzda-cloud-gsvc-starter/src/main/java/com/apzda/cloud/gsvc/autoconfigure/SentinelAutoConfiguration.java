package com.apzda.cloud.gsvc.autoconfigure;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpClient;
import com.apzda.cloud.adapter.servlet.CommonFilter;
import com.apzda.cloud.adapter.servlet.callback.UrlBlockHandler;
import com.apzda.cloud.adapter.servlet.callback.WebCallbackManager;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.plugin.SentinelPlugin;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author fengz
 */
@AutoConfiguration(before = ApzdaGsvcAutoConfiguration.class)
@ConditionalOnClass(SimpleHttpClient.class)
@Slf4j
public class SentinelAutoConfiguration {

    @Bean
    SentinelPlugin gsvcSentinelPlugin() {
        return new SentinelPlugin();
    }

    @Bean
    public FilterRegistrationBean<Filter> sentinelFilterRegistration() {
        WebCallbackManager.setUrlCleaner(originUrl -> {
            if (originUrl == null || originUrl.isEmpty()) {
                return originUrl;
            }

            return originUrl;
        });
        WebCallbackManager.setRequestOriginParser(
                request -> StringUtils.defaultIfBlank(request.getHeader("x-gsvc-caller"), "default"));
        WebCallbackManager.setUrlBlockHandler(new UrlBlockHandler() {
            @Override
            public void blocked(HttpServletRequest request, HttpServletResponse response, BlockException ex)
                    throws IOException {

                val requestId = StringUtils.defaultIfBlank((String) request.getAttribute("X-Request-Id"),
                        UUID.randomUUID().toString());

                if (log.isDebugEnabled()) {
                    val caller = request.getHeader("x-gsvc-caller");
                    log.debug("[{}] visit {} from {} is blocked by Sentinel", requestId, request.getRequestURI(),
                            caller);
                }

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
        });
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CommonFilter());
        registration.addUrlPatterns("/*");
        registration.setName("gsvcSentinelFilter");
        registration.setOrder(2);

        return registration;
    }

}
