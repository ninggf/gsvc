package com.apzda.cloud.demo.bar.controller;

import com.apzda.cloud.demo.math.proto.MathService;
import com.apzda.cloud.demo.math.proto.OpNum;
import com.apzda.cloud.demo.math.proto.Request;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.security.token.SimpleJwtToken;
import com.apzda.cloud.gsvc.security.token.TokenManager;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
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

    private final MathService mathService;

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
    public String i18n(@PathVariable String key, @RequestParam(value = "name", required = false) String name) {
        return I18nUtils.t(key) + (StringUtils.isNotBlank(name) ? " " + name : "");
    }

    @GetMapping("/gi18n/{key}")
    public String gi18n(@PathVariable String key) {
        val resp = mathService.translate(Request.newBuilder().setKey(key).build());
        return resp.getMessage();
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

    @GetMapping("/authz")
    public Response<String> auth() {
        val resp = mathService.auth(OpNum.newBuilder().setNum1(1).setNum2(1).build());
        return Response.success(resp.getMessage());
    }

    @GetMapping("/authx")
    public Response<String> authx() {
        mathService.authz(OpNum.newBuilder().setNum1(1).setNum2(1).build());
        return Response.success("ok");
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
