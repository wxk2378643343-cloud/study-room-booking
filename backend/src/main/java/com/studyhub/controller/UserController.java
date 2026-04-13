package com.studyhub.controller;

import com.studyhub.auth.AuthContext;
import com.studyhub.common.ApiResponse;
import com.studyhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthContext authContext;

    public UserController(UserService userService, AuthContext authContext) {
        this.userService = userService;
        this.authContext = authContext;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        return ApiResponse.ok(userService.profile(authContext.currentUserId(request)));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        userService.updateProfile(
            authContext.currentUserId(request),
            String.valueOf(body.getOrDefault("nickname", "用户")),
            body.get("preferredRoomId") == null ? null : ((Number) body.get("preferredRoomId")).longValue()
        );
        return ApiResponse.ok(null);
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        userService.changePassword(
            authContext.currentUserId(request),
            body.getOrDefault("oldPassword", ""),
            body.getOrDefault("newPassword", "")
        );
        return ApiResponse.ok(null);
    }

    @GetMapping("/violations")
    public ApiResponse<List<Map<String, Object>>> violations(HttpServletRequest request) {
        return ApiResponse.ok(userService.violations(authContext.currentUserId(request)));
    }

    @GetMapping("/points")
    public ApiResponse<Map<String, Object>> points(HttpServletRequest request) {
        return ApiResponse.ok(userService.points(authContext.currentUserId(request)));
    }

    @GetMapping("/points/logs")
    public ApiResponse<List<Map<String, Object>>> pointsLogs(HttpServletRequest request) {
        return ApiResponse.ok(userService.pointLogs(authContext.currentUserId(request)));
    }

    @GetMapping("/coupons")
    public ApiResponse<List<Map<String, Object>>> coupons(HttpServletRequest request) {
        return ApiResponse.ok(userService.coupons(authContext.currentUserId(request)));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> notifications(HttpServletRequest request) {
        return ApiResponse.ok(userService.notifications(authContext.currentUserId(request)));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Integer> unreadCount(HttpServletRequest request) {
        return ApiResponse.ok(userService.unreadCount(authContext.currentUserId(request)));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id, HttpServletRequest request) {
        userService.readNotification(authContext.currentUserId(request), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/reports")
    public ApiResponse<Void> report(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        userService.createReport(
            authContext.currentUserId(request),
            body.get("orderId") == null ? null : ((Number) body.get("orderId")).longValue(),
            ((Number) body.get("seatId")).longValue(),
            String.valueOf(body.getOrDefault("imageUrl", "")),
            String.valueOf(body.getOrDefault("description", ""))
        );
        return ApiResponse.ok(null);
    }
}
