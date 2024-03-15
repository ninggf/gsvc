package com.apzda.cloud.demo.bar.controller;

import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.security.token.JwtToken;
import com.apzda.cloud.gsvc.security.token.SimpleJwtToken;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.apzda.cloud.gsvc.utils.I18nHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
    public ResponseEntity<Response<?>> refresh(@RequestBody SimpleJwtToken token) {
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

    @GetMapping("/add")
    @Validated
    public String add(@Valid @Max(10) Integer i, @Valid @Min(10) Integer j) {
        return String.valueOf(i + j);
    }

    @PostMapping("/demo")
    @Validated
    public Response<DemoDto> demo(@Valid @RequestBody DemoDto demoDto) {
        return Response.success(demoDto);
    }

    @Data
    public static class DemoDto {

        @NotNull
        @Max(10)
        private Integer num1;

        @NotBlank
        private String abc;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate date;

    }

}
