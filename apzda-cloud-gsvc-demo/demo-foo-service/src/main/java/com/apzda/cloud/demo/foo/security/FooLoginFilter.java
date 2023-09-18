package com.apzda.cloud.demo.foo.security;

import com.apzda.cloud.gsvc.security.authentication.DeviceAwareAuthenticationProcessingFilter;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author fengz
 */
@Component
public class FooLoginFilter extends DeviceAwareAuthenticationProcessingFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher(
            "/foobar/login", "POST");

    public FooLoginFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        val username = request.getParameter("username");
        val password = request.getParameter("password");
        val token = JwtAuthenticationToken.unauthenticated(username, password);

        setDetails(request, token);
        // 开始认证
        return this.getAuthenticationManager().authenticate(token);
    }

}
