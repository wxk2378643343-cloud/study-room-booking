package com.studyhub.controller;

import com.studyhub.auth.AuthContext;
import com.studyhub.common.ApiResponse;
import com.studyhub.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final AuthContext authContext;

    public RoomController(RoomService roomService, AuthContext authContext) {
        this.roomService = roomService;
        this.authContext = authContext;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listRooms() {
        return ApiResponse.ok(roomService.listRooms());
    }

    @GetMapping("/{roomId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long roomId) {
        return ApiResponse.ok(roomService.roomDetail(roomId));
    }

    @GetMapping("/{roomId}/seat-slots")
    public ApiResponse<List<Map<String, Object>>> slots(@PathVariable Long roomId,
                                                        @RequestParam String date,
                                                        @RequestParam Integer slotCode) {
        return ApiResponse.ok(roomService.seatSlots(roomId, LocalDate.parse(date), slotCode));
    }

    @PostMapping("/{roomId}/reviews")
    public ApiResponse<Void> review(@PathVariable Long roomId,
                                    @RequestBody Map<String, Object> body,
                                    HttpServletRequest request) {
        Long userId = authContext.currentUserId(request);
        roomService.addReview(
            userId,
            roomId,
            ((Number) body.get("score")).intValue(),
            String.valueOf(body.getOrDefault("content", "")),
            body.get("orderId") == null ? null : ((Number) body.get("orderId")).longValue()
        );
        return ApiResponse.ok(null);
    }
}
