package com.studyhub.controller;

import com.studyhub.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "studyhub-backend");
        result.put("status", "UP");
        result.put("time", LocalDateTime.now().toString());
        return ApiResponse.ok(result);
    }
}
