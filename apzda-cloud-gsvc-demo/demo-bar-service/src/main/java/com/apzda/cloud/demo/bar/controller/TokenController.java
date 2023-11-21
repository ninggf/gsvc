package com.apzda.cloud.demo.bar.controller;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.i18n.I18nHelper;
import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author fengz
 */
@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class TokenController {

    private final TokenManager tokenManager;

    private final MessageSource messageSource;

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

    @GetMapping("/i18n/{key}")
    public String i18n(@PathVariable String key, HttpServletRequest request) {
        return I18nHelper.t(key);
    }

}
