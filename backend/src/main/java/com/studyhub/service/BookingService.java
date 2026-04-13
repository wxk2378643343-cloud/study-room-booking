package com.studyhub.service;

import com.studyhub.exception.BizException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    private final JdbcTemplate jdbcTemplate;

    public BookingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> createBooking(Long userId, Long roomId, Long seatId, LocalDate date, Integer slotCode) {
        Map<String, Object> user = jdbcTemplate.queryForMap("SELECT violation_count, booking_restricted_until FROM sys_user WHERE id = ?", userId);
        Object restricted = user.get("booking_restricted_until");
        if (restricted != null) {
            LocalDateTime until;
            if (restricted instanceof java.sql.Timestamp ts) {
                until = ts.toLocalDateTime();
            } else {
                until = LocalDateTime.parse(restricted.toString().replace(" ", "T"));
            }
            if (until.isAfter(LocalDateTime.now())) {
                throw new BizException("当前处于限制预约期");
            }
        }

        Integer dailyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM booking_order WHERE user_id = ? AND booking_date = ? AND status IN (0,1)",
            Integer.class, userId, date
        );
        Integer maxPerDay = queryRuleOrDefault("max_bookings_per_day", roomId, 2);
        if (dailyCount != null && dailyCount >= maxPerDay) {
            throw new BizException("超过每日预约上限");
        }

        Integer seatOpen = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM seat WHERE id = ? AND room_id = ? AND status = 1",
            Integer.class, seatId, roomId);
        if (seatOpen == null || seatOpen == 0) {
            throw new BizException("座位不可用");
        }

        // Ensure row exists then lock it by update condition.
        jdbcTemplate.update(
            "INSERT INTO seat_slot(seat_id, booking_date, slot_code, status) VALUES (?, ?, ?, 0) " +
                "ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP",
            seatId, date, slotCode
        );
        int affected = jdbcTemplate.update(
            "UPDATE seat_slot SET status = 1, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ? AND status = 0",
            seatId, date, slotCode
        );
        if (affected == 0) {
            throw new BizException("该座位时段已被占用");
        }

        LocalDateTime start = slotStart(date, slotCode);
        LocalDateTime end = slotEnd(date, slotCode);
        String orderNo = "BK" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + UUID.randomUUID().toString().substring(0, 6);
        String voucher = "VCH-" + UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO booking_order(order_no, user_id, room_id, seat_id, booking_date, slot_code, start_time, end_time, status, voucher_code) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)",
            orderNo, userId, roomId, seatId, date, slotCode, start, end, voucher
        );
        Long orderId = jdbcTemplate.queryForObject("SELECT id FROM booking_order WHERE order_no = ?", Long.class, orderNo);
        jdbcTemplate.update("UPDATE seat_slot SET current_order_id = ? WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
            orderId, seatId, date, slotCode);
        jdbcTemplate.update(
            "INSERT INTO notification(user_id, msg_type, title, content) VALUES (?, 1, '预约成功', ?)",
            userId, "预约成功，座位#" + seatId + "，日期" + date + "时段" + slotCode
        );
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderNo", orderNo);
        result.put("voucherCode", voucher);
        result.put("startTime", start);
        result.put("endTime", end);
        return result;
    }

    public List<Map<String, Object>> myBookings(Long userId) {
        return jdbcTemplate.queryForList(
            "SELECT bo.*, sr.name room_name, s.seat_no, " +
                "CASE bo.slot_code " +
                "WHEN 1 THEN '08:00-10:00' " +
                "WHEN 2 THEN '10:00-12:00' " +
                "WHEN 3 THEN '13:00-15:00' " +
                "WHEN 4 THEN '15:00-17:00' " +
                "WHEN 5 THEN '18:00-20:00' " +
                "WHEN 6 THEN '20:00-22:00' " +
                "ELSE CONCAT('时段', bo.slot_code) END AS slot_text " +
                "FROM booking_order bo " +
                "JOIN study_room sr ON bo.room_id = sr.id " +
                "JOIN seat s ON bo.seat_id = s.id WHERE bo.user_id = ? ORDER BY bo.id DESC",
            userId
        );
    }

    @Transactional
    public void cancelBooking(Long userId, Long orderId) {
        Map<String, Object> order = jdbcTemplate.queryForMap(
            "SELECT id, seat_id, booking_date, slot_code, status FROM booking_order WHERE id = ? AND user_id = ?",
            orderId, userId
        );
        int status = ((Number) order.get("status")).intValue();
        if (status != 0) {
            throw new BizException("仅待签到订单可取消");
        }
        jdbcTemplate.update("UPDATE booking_order SET status = 3, cancel_reason = ? WHERE id = ?", "用户主动取消", orderId);
        jdbcTemplate.update(
            "UPDATE seat_slot SET status = 0, current_order_id = NULL, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
            order.get("seat_id"), order.get("booking_date"), order.get("slot_code")
        );
        jdbcTemplate.update(
            "INSERT INTO notification(user_id, msg_type, title, content) VALUES (?, 2, '预约已取消', ?)",
            userId, "订单#" + orderId + "已取消"
        );
    }

    @Transactional
    public void checkIn(Long userId, String voucherCode) {
        Map<String, Object> order;
        try {
            order = jdbcTemplate.queryForMap("SELECT * FROM booking_order WHERE voucher_code = ? AND user_id = ?", voucherCode, userId);
        } catch (EmptyResultDataAccessException e) {
            throw new BizException("签到凭证无效或不属于当前用户");
        }
        int status = ((Number) order.get("status")).intValue();
        if (status != 0) {
            throw new BizException("当前订单不可签到");
        }
        LocalDateTime startTime = toLdt(order.get("start_time"));
        Integer grace = queryRuleOrDefault("checkin_grace_minutes", ((Number) order.get("room_id")).longValue(), 120);
        if (LocalDateTime.now().isAfter(startTime.plusMinutes(grace))) {
            jdbcTemplate.update("UPDATE booking_order SET status = 4, cancel_reason = '超时未签到' WHERE id = ?", order.get("id"));
            jdbcTemplate.update(
                "UPDATE seat_slot SET status = 0, current_order_id = NULL, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
                order.get("seat_id"), order.get("booking_date"), order.get("slot_code")
            );
            recordViolation(userId, ((Number) order.get("id")).longValue(), 1, "超时未签到");
            throw new BizException("签到超时，订单已取消");
        }
        jdbcTemplate.update("UPDATE booking_order SET status = 1 WHERE id = ?", order.get("id"));
        jdbcTemplate.update(
            "INSERT INTO checkin_record(order_id, checkin_time) VALUES (?, NOW()) ON DUPLICATE KEY UPDATE checkin_time = NOW()",
            order.get("id")
        );
    }

    @Transactional
    public void temporaryLeave(Long userId, Long orderId) {
        Map<String, Object> order = jdbcTemplate.queryForMap("SELECT * FROM booking_order WHERE id = ? AND user_id = ?", orderId, userId);
        int status = ((Number) order.get("status")).intValue();
        if (status != 1) {
            throw new BizException("仅使用中订单可临时离开");
        }
        Integer leaveMinutes = queryRuleOrDefault("temp_leave_minutes", ((Number) order.get("room_id")).longValue(), 15);
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO temp_leave_record(order_id, leave_start, expected_back_time) VALUES (?, ?, ?)",
            orderId, now, now.plusMinutes(leaveMinutes)
        );
        jdbcTemplate.update(
            "UPDATE seat_slot SET status = 2, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
            order.get("seat_id"), order.get("booking_date"), order.get("slot_code")
        );
    }

    @Transactional
    public void returnFromLeave(Long userId, Long orderId) {
        Map<String, Object> order = jdbcTemplate.queryForMap("SELECT * FROM booking_order WHERE id = ? AND user_id = ?", orderId, userId);
        jdbcTemplate.update(
            "UPDATE temp_leave_record SET actual_back_time = NOW() WHERE order_id = ? AND actual_back_time IS NULL ORDER BY id DESC LIMIT 1",
            orderId
        );
        jdbcTemplate.update(
            "UPDATE seat_slot SET status = 1, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
            order.get("seat_id"), order.get("booking_date"), order.get("slot_code")
        );
    }

    @Transactional
    public void checkOut(Long userId, Long orderId) {
        Map<String, Object> order = jdbcTemplate.queryForMap("SELECT * FROM booking_order WHERE id = ? AND user_id = ?", orderId, userId);
        int status = ((Number) order.get("status")).intValue();
        if (status != 1) {
            throw new BizException("当前订单不可签退");
        }
        jdbcTemplate.update("UPDATE booking_order SET status = 2 WHERE id = ?", orderId);
        jdbcTemplate.update("UPDATE checkin_record SET checkout_time = NOW() WHERE order_id = ?", orderId);
        jdbcTemplate.update(
            "UPDATE seat_slot SET status = 0, current_order_id = NULL, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
            order.get("seat_id"), order.get("booking_date"), order.get("slot_code")
        );
        jdbcTemplate.update("UPDATE points_account SET points_balance = points_balance + 10 WHERE user_id = ?", userId);
        jdbcTemplate.update("INSERT INTO points_txn(user_id, change_amount, biz_type, biz_ref_id) VALUES (?, 10, 1, ?)", userId, orderId);
    }

    @Transactional
    public void runTimeoutJobs() {
        // no-show cancellation
        List<Map<String, Object>> overdue = jdbcTemplate.queryForList(
            "SELECT bo.id, bo.user_id, bo.room_id, bo.seat_id, bo.booking_date, bo.slot_code " +
                "FROM booking_order bo JOIN booking_rule br ON bo.room_id = br.room_id " +
                "WHERE bo.status = 0 AND NOW() > DATE_ADD(bo.start_time, INTERVAL br.checkin_grace_minutes MINUTE)"
        );
        for (Map<String, Object> item : overdue) {
            Long orderId = ((Number) item.get("id")).longValue();
            Long userId = ((Number) item.get("user_id")).longValue();
            jdbcTemplate.update("UPDATE booking_order SET status = 4, cancel_reason = '超时未签到' WHERE id = ?", orderId);
            jdbcTemplate.update(
                "UPDATE seat_slot SET status = 0, current_order_id = NULL, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
                item.get("seat_id"), item.get("booking_date"), item.get("slot_code")
            );
            recordViolation(userId, orderId, 1, "超时未签到");
        }

        // leave timeout
        List<Map<String, Object>> timeoutLeave = jdbcTemplate.queryForList(
            "SELECT tl.order_id, bo.user_id, bo.seat_id, bo.booking_date, bo.slot_code " +
                "FROM temp_leave_record tl JOIN booking_order bo ON tl.order_id = bo.id " +
                "WHERE tl.actual_back_time IS NULL AND tl.is_timeout = 0 AND NOW() > tl.expected_back_time"
        );
        for (Map<String, Object> item : timeoutLeave) {
            Long orderId = ((Number) item.get("order_id")).longValue();
            Long userId = ((Number) item.get("user_id")).longValue();
            jdbcTemplate.update("UPDATE temp_leave_record SET is_timeout = 1 WHERE order_id = ? AND actual_back_time IS NULL", orderId);
            jdbcTemplate.update("UPDATE booking_order SET status = 3, cancel_reason = '临时离开超时' WHERE id = ? AND status = 1", orderId);
            jdbcTemplate.update(
                "UPDATE seat_slot SET status = 0, current_order_id = NULL, version = version + 1 WHERE seat_id = ? AND booking_date = ? AND slot_code = ?",
                item.get("seat_id"), item.get("booking_date"), item.get("slot_code")
            );
            recordViolation(userId, orderId, 3, "临时离开超时");
        }
    }

    private void recordViolation(Long userId, Long orderId, int type, String remark) {
        jdbcTemplate.update("INSERT INTO violation_record(user_id, order_id, violation_type, remark) VALUES (?, ?, ?, ?)",
            userId, orderId, type, remark);
        jdbcTemplate.update("UPDATE sys_user SET violation_count = violation_count + 1 WHERE id = ?", userId);
        Integer count = jdbcTemplate.queryForObject("SELECT violation_count FROM sys_user WHERE id = ?", Integer.class, userId);
        if (count != null && count >= 3) {
            jdbcTemplate.update("UPDATE sys_user SET booking_restricted_until = DATE_ADD(NOW(), INTERVAL 7 DAY) WHERE id = ?", userId);
        }
    }

    private LocalDateTime slotStart(LocalDate d, Integer slotCode) {
        return switch (slotCode) {
            case 1 -> LocalDateTime.of(d, LocalTime.of(8, 0));
            case 2 -> LocalDateTime.of(d, LocalTime.of(10, 0));
            case 3 -> LocalDateTime.of(d, LocalTime.of(13, 0));
            case 4 -> LocalDateTime.of(d, LocalTime.of(15, 0));
            case 5 -> LocalDateTime.of(d, LocalTime.of(18, 0));
            case 6 -> LocalDateTime.of(d, LocalTime.of(20, 0));
            default -> throw new BizException("slotCode 必须为 1-6");
        };
    }

    private LocalDateTime slotEnd(LocalDate d, Integer slotCode) {
        return switch (slotCode) {
            case 1 -> LocalDateTime.of(d, LocalTime.of(10, 0));
            case 2 -> LocalDateTime.of(d, LocalTime.of(12, 0));
            case 3 -> LocalDateTime.of(d, LocalTime.of(15, 0));
            case 4 -> LocalDateTime.of(d, LocalTime.of(17, 0));
            case 5 -> LocalDateTime.of(d, LocalTime.of(20, 0));
            case 6 -> LocalDateTime.of(d, LocalTime.of(22, 0));
            default -> throw new BizException("slotCode 必须为 1-6");
        };
    }

    private LocalDateTime toLdt(Object val) {
        if (val instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (val instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return LocalDateTime.parse(val.toString().replace(" ", "T"));
    }

    private Integer queryRuleOrDefault(String column, Long roomId, int defaultValue) {
        try {
            Integer v = jdbcTemplate.queryForObject("SELECT " + column + " FROM booking_rule WHERE room_id = ?", Integer.class, roomId);
            return v == null ? defaultValue : v;
        } catch (EmptyResultDataAccessException e) {
            return defaultValue;
        }
    }
}
