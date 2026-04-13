package com.studyhub.controller.admin;

import com.studyhub.auth.AuthContext;
import com.studyhub.common.ApiResponse;
import com.studyhub.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthContext authContext;

    public AdminController(AdminService adminService, AuthContext authContext) {
        this.adminService = adminService;
        this.authContext = authContext;
    }

    @GetMapping("/rooms")
    public ApiResponse<Map<String, Object>> rooms(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        return ApiResponse.ok(adminService.listRoomsPaged(page, pageSize));
    }

    @PostMapping("/rooms")
    public ApiResponse<Void> saveRoom(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.saveRoom(
            body.get("id") == null ? null : ((Number) body.get("id")).longValue(),
            String.valueOf(body.get("name")),
            String.valueOf(body.get("locationDesc")),
            String.valueOf(body.getOrDefault("facilities", "")),
            ((Number) body.getOrDefault("seatTotal", 0)).intValue(),
            ((Number) body.getOrDefault("status", 1)).intValue()
        );
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ApiResponse<Void> deleteRoom(@PathVariable Long roomId, HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.deleteRoom(roomId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/rooms/{roomId}/seats")
    public ApiResponse<Map<String, Object>> seats(@PathVariable Long roomId,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        return ApiResponse.ok(adminService.listSeatsPaged(roomId, page, pageSize));
    }

    @PostMapping("/rooms/{roomId}/seats")
    public ApiResponse<Void> saveSeat(@PathVariable Long roomId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.saveSeat(
            body.get("id") == null ? null : ((Number) body.get("id")).longValue(),
            roomId,
            String.valueOf(body.get("seatNo")),
            ((Number) body.getOrDefault("seatType", 1)).intValue(),
            ((Number) body.getOrDefault("xPos", 0)).intValue(),
            ((Number) body.getOrDefault("yPos", 0)).intValue(),
            ((Number) body.getOrDefault("status", 1)).intValue()
        );
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/rooms/{roomId}/seats/{seatId}")
    public ApiResponse<Void> deleteSeat(@PathVariable Long roomId,
                                        @PathVariable Long seatId,
                                        HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.deleteSeat(roomId, seatId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/rooms/{roomId}/rule")
    public ApiResponse<Map<String, Object>> rule(@PathVariable Long roomId, HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        return ApiResponse.ok(adminService.bookingRule(roomId));
    }

    @PostMapping("/rooms/{roomId}/rule")
    public ApiResponse<Void> saveRule(@PathVariable Long roomId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.saveBookingRule(
            roomId,
            ((Number) body.getOrDefault("maxBookingsPerDay", 2)).intValue(),
            ((Number) body.getOrDefault("maxHoursPerBooking", 4)).intValue(),
            ((Number) body.getOrDefault("checkinGraceMinutes", 15)).intValue(),
            ((Number) body.getOrDefault("tempLeaveMinutes", 15)).intValue()
        );
        return ApiResponse.ok(null);
    }

    @GetMapping("/reports")
    public ApiResponse<Map<String, Object>> reports(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        return ApiResponse.ok(adminService.reportsPaged(page, pageSize));
    }

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> users(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        return ApiResponse.ok(adminService.listUsersPaged(page, pageSize));
    }

    @PostMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId,
                                              @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.updateUserStatus(userId, ((Number) body.getOrDefault("status", 1)).intValue());
        return ApiResponse.ok(null);
    }

    @PostMapping("/users/{userId}")
    public ApiResponse<Void> updateUser(@PathVariable Long userId,
                                        @RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.updateUser(
            userId,
            String.valueOf(body.getOrDefault("phone", "")),
            String.valueOf(body.getOrDefault("nickname", "")),
            ((Number) body.getOrDefault("role", 1)).intValue(),
            ((Number) body.getOrDefault("status", 1)).intValue(),
            ((Number) body.getOrDefault("violationCount", 0)).intValue()
        );
        return ApiResponse.ok(null);
    }

    @PostMapping("/users/{userId}/reset-violations")
    public ApiResponse<Void> resetUserViolations(@PathVariable Long userId, HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        adminService.resetUserViolations(userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/reports/{reportId}/handle")
    public ApiResponse<Void> handleReport(@PathVariable Long reportId,
                                          @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        Long adminId = authContext.currentUserId(request);
        adminService.assertAdmin(adminId);
        adminService.handleReport(
            adminId,
            reportId,
            ((Number) body.getOrDefault("status", 2)).intValue(),
            String.valueOf(body.getOrDefault("note", ""))
        );
        return ApiResponse.ok(null);
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam String from,
                                                      @RequestParam String to,
                                                      HttpServletRequest request) {
        adminService.assertAdmin(authContext.currentUserId(request));
        return ApiResponse.ok(adminService.dashboard(LocalDate.parse(from), LocalDate.parse(to)));
    }
}
