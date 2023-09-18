package com.apzda.cloud.demo.bar.controller;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz
 */
@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class TokenController {

    private final TokenManager tokenManager;

    @PostMapping("/refresh")
    public ResponseEntity<Response<?>> refresh(@RequestBody JwtToken token) {
        val jwtToken = tokenManager.refreshAccessToken(token);
        if (jwtToken != null) {
            return ResponseEntity.ok(Response.success(jwtToken));
        }
        else {
            return ResponseEntity.status(500).body(Response.error(500, "Cannot refresh accessToken"));
        }
    }

}
