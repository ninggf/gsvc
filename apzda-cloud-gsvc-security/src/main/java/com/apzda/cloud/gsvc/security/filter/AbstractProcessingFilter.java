package com.apzda.cloud.gsvc.security.filter;

import com.apzda.cloud.gsvc.security.authentication.DeviceAuthenticationDetailsSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * @author fengz
 */
@Slf4j
public abstract class AbstractProcessingFilter extends AbstractAuthenticationProcessingFilter
        implements Ordered, ApplicationContextAware {

    protected AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new DeviceAuthenticationDetailsSource();

    protected ApplicationContext applicationContext;

    protected ObjectMapper objectMapper;

    protected AbstractProcessingFilter(String defaultFilterProcessesUrl) {
        super(defaultFilterProcessesUrl, null);
    }

    protected AbstractProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher, null);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.objectMapper = applicationContext.getBean(ObjectMapper.class);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    protected void setDetails(HttpServletRequest request, AbstractAuthenticationToken authRequest) {
        if (authRequest.getDetails() == null) {
            authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
        }
    }

    protected <R> R readRequestBody(HttpServletRequest request, Class<R> rClass) {
        val req = new ContentCachingRequestWrapper(request);

        try (val reader = req.getReader()) {
            val stringBuilder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = reader.readLine();
            }
            return objectMapper.readValue(stringBuilder.toString(), rClass);
        }
        catch (IOException e) {
            log.error("Cannot read the request body from: {}", request.getRequestURI());
            return null;
        }
    }

}
