package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.*;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author fengz
 */
@Service
@RequiredArgsConstructor
public class SaServiceImpl implements SaService {

    private final TokenManager tokenManager;

    private final AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper;

    @Override
    public LoginRes login(LoginReq request) {
        val username = request.getUsername();
        val password = request.getPassword();

        try {

            val authRequest = JwtAuthenticationToken.unauthenticated(username, password);
            val authenticate = authenticationManager.authenticate(authRequest);

            if (authenticate != null && authenticate.isAuthenticated()) {
                val jwtToken = tokenManager.createJwtToken(authenticate);

                if (authenticate instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                    jwtAuthenticationToken.login(jwtToken);
                }

                val context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
                context.setAuthentication(authenticate);
                SecurityContextHolder.setContext(context);

                return LoginRes.newBuilder()
                    .setErrCode(0)
                    .setAccessToken(jwtToken.getAccessToken())
                    .setRefreshToken(jwtToken.getRefreshToken())
                    .setName(jwtToken.getName())
                    .build();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new ResponseStatusException(HttpStatusCode.valueOf(401));
    }

    @Override
    @PreAuthorize("hasPermission(filterObject.name == authentication.name,'view:/sa/info')")
    public SaRes info(SaReq request) {
        var uid = "";
        if (request.hasCurrentUser()) {
            uid = request.getCurrentUser().getUid();
        }
        return SaRes.newBuilder().setUserName(request.getName() + "." + uid).setUid(uid).setErrCode(0).build();
    }

    @Override
    public SaRes hi(SaReq request) {

        return SaRes.newBuilder()
            .setUserName(request.getName() + ", " + request.getCurrentUser().getUid())
            .setErrCode(0)
            .build();
    }

}
