package com.studyhub.controller;

import com.studyhub.auth.AuthContext;
import com.studyhub.common.ApiResponse;
import com.studyhub.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final AuthContext authContext;

    public BookingController(BookingService bookingService, AuthContext authContext) {
        this.bookingService = bookingService;
        this.authContext = authContext;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = authContext.currentUserId(request);
        return ApiResponse.ok(bookingService.createBooking(
            userId,
            ((Number) body.get("roomId")).longValue(),
            ((Number) body.get("seatId")).longValue(),
            LocalDate.parse(String.valueOf(body.get("date"))),
            ((Number) body.get("slotCode")).intValue()
        ));
    }

    @GetMapping("/mine")
    public ApiResponse<List<Map<String, Object>>> mine(HttpServletRequest request) {
        return ApiResponse.ok(bookingService.myBookings(authContext.currentUserId(request)));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long orderId, HttpServletRequest request) {
        bookingService.cancelBooking(authContext.currentUserId(request), orderId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/checkin")
    public ApiResponse<Void> checkIn(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        bookingService.checkIn(authContext.currentUserId(request), String.valueOf(body.get("voucherCode")));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{orderId}/leave")
    public ApiResponse<Void> leave(@PathVariable Long orderId, HttpServletRequest request) {
        bookingService.temporaryLeave(authContext.currentUserId(request), orderId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{orderId}/return")
    public ApiResponse<Void> back(@PathVariable Long orderId, HttpServletRequest request) {
        bookingService.returnFromLeave(authContext.currentUserId(request), orderId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{orderId}/checkout")
    public ApiResponse<Void> checkout(@PathVariable Long orderId, HttpServletRequest request) {
        bookingService.checkOut(authContext.currentUserId(request), orderId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/jobs/run-timeout")
    public ApiResponse<Void> runTimeout() {
        bookingService.runTimeoutJobs();
        return ApiResponse.ok(null);
    }
}
