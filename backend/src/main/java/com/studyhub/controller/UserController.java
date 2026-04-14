package com.studyhub.controller;

import com.studyhub.auth.AuthContext;
import com.studyhub.common.ApiResponse;
import com.studyhub.exception.BizException;
import com.studyhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
            ((Number) body.getOrDefault("reportType", 2)).intValue(),
            String.valueOf(body.getOrDefault("imageUrl", "")),
            String.valueOf(body.getOrDefault("description", ""))
        );
        return ApiResponse.ok(null);
    }

    @PostMapping(value = "/report-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadReportImage(@RequestParam("file") MultipartFile file,
                                                              HttpServletRequest request) {
        if (file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw new BizException("图片不能超过5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException("只支持图片文件");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(extension)) {
            extension = ".jpg";
        }

        Path uploadDir = Paths.get("uploads", "reports").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> result = new HashMap<>();
            result.put("url", "/uploads/reports/" + filename);
            result.put("filename", filename);
            return ApiResponse.ok(result);
        } catch (IOException e) {
            throw new BizException("图片上传失败");
        }
    }
}
