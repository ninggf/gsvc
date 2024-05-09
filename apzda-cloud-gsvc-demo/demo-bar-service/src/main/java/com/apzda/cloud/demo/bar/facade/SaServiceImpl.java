package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.*;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.token.JwtTokenCustomizer;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.google.protobuf.Empty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author fengz
 */
@Service
@RequiredArgsConstructor
@Tag(name = "SaService")
public class SaServiceImpl implements SaService {

    private final TokenManager tokenManager;

    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository;

    private final ObjectProvider<JwtTokenCustomizer> customizers;

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
                securityContextRepository.saveContext(context, GsvcContextHolder.getRequest().get(),
                        GsvcContextHolder.getResponse().get());

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

    @Override
    @PreAuthorize("isAuthenticated()")
    public GsvcExt.CommonRes active(Empty request) {
        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public GsvcExt.CommonRes reset(Empty request) {
        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public GsvcExt.CommonRes setup(Empty request) {
        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

    @Override
    @PreAuthorize("hasRole('sa')")
    public GsvcExt.CommonRes deny(Empty request) {
        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

}
