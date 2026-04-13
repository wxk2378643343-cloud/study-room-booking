package com.studyhub.controller;

import com.studyhub.auth.AuthContext;
import com.studyhub.common.ApiResponse;
import com.studyhub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthContext authContext;

    public AuthController(AuthService authService, AuthContext authContext) {
        this.authService = authService;
        this.authContext = authContext;
    }

    @PostMapping("/send-code")
    public ApiResponse<Map<String, Object>> sendCode(@RequestParam @NotBlank String phone) {
        return ApiResponse.ok(authService.sendCode(phone));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.register(
            body.get("phone"),
            body.get("code"),
            body.get("password"),
            body.getOrDefault("nickname", "新用户")
        ));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.login(body.get("phone"), body.get("password")));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.ok(authService.me(authContext.currentUserId(request)));
    }
}
