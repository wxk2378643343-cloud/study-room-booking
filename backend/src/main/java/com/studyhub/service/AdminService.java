package com.studyhub.service;

import com.studyhub.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final JdbcTemplate jdbcTemplate;

    public AdminService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listRooms() {
        return jdbcTemplate.queryForList("SELECT * FROM study_room ORDER BY id");
    }

    public Map<String, Object> listRoomsPaged(int page, int pageSize) {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM study_room", Integer.class);
        int p = Math.max(page, 1);
        int size = Math.max(pageSize, 1);
        int offset = (p - 1) * size;
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT * FROM study_room ORDER BY id LIMIT ? OFFSET ?",
            size, offset
        );
        return pageResult(items, total == null ? 0 : total, p, size);
    }

    public void saveRoom(Long id, String name, String location, String facilities, Integer seatTotal, Integer status) {
        if (id == null) {
            jdbcTemplate.update("INSERT INTO study_room(name, location_desc, facilities, seat_total, status) VALUES (?, ?, ?, ?, ?)",
                name, location, facilities, seatTotal, status);
        } else {
            jdbcTemplate.update("UPDATE study_room SET name=?, location_desc=?, facilities=?, seat_total=?, status=? WHERE id=?",
                name, location, facilities, seatTotal, status, id);
        }
    }

    public void deleteRoom(Long roomId) {
        Integer hasOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM booking_order WHERE room_id = ?",
            Integer.class,
            roomId
        );
        if (hasOrders != null && hasOrders > 0) {
            throw new BizException("该房间存在预约订单，无法删除");
        }

        jdbcTemplate.update(
            "UPDATE user_profile SET preferred_room_id = NULL WHERE preferred_room_id = ?",
            roomId
        );
        jdbcTemplate.update("DELETE FROM room_open_rule WHERE room_id = ?", roomId);
        jdbcTemplate.update("DELETE FROM booking_rule WHERE room_id = ?", roomId);
        jdbcTemplate.update("DELETE FROM seat_slot WHERE seat_id IN (SELECT id FROM seat WHERE room_id = ?)", roomId);
        jdbcTemplate.update("DELETE FROM seat WHERE room_id = ?", roomId);
        jdbcTemplate.update("DELETE FROM study_room WHERE id = ?", roomId);
    }

    public List<Map<String, Object>> listSeats(Long roomId) {
        return jdbcTemplate.queryForList("SELECT * FROM seat WHERE room_id = ? ORDER BY id", roomId);
    }

    public Map<String, Object> listSeatsPaged(Long roomId, int page, int pageSize) {
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM seat WHERE room_id = ?",
            Integer.class,
            roomId
        );
        int p = Math.max(page, 1);
        int size = Math.max(pageSize, 1);
        int offset = (p - 1) * size;
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT * FROM seat WHERE room_id = ? ORDER BY id LIMIT ? OFFSET ?",
            roomId, size, offset
        );
        return pageResult(items, total == null ? 0 : total, p, size);
    }

    public void saveSeat(Long id, Long roomId, String seatNo, Integer seatType, Integer x, Integer y, Integer status) {
        if (id == null) {
            jdbcTemplate.update(
                "INSERT INTO seat(room_id, seat_no, seat_type, x_pos, y_pos, status) VALUES (?, ?, ?, ?, ?, ?)",
                roomId, seatNo, seatType, x, y, status
            );
        } else {
            jdbcTemplate.update(
                "UPDATE seat SET seat_no=?, seat_type=?, x_pos=?, y_pos=?, status=? WHERE id=? AND room_id=?",
                seatNo, seatType, x, y, status, id, roomId
            );
        }
    }

    public void deleteSeat(Long roomId, Long seatId) {
        Integer hasOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM booking_order WHERE room_id = ? AND seat_id = ?",
            Integer.class,
            roomId,
            seatId
        );
        if (hasOrders != null && hasOrders > 0) {
            throw new BizException("该座位存在预约订单，无法删除");
        }

        jdbcTemplate.update("DELETE FROM seat_slot WHERE seat_id = ?", seatId);
        jdbcTemplate.update("DELETE FROM violation_report WHERE seat_id = ?", seatId);
        jdbcTemplate.update("DELETE FROM seat WHERE id = ? AND room_id = ?", seatId, roomId);
    }

    public Map<String, Object> bookingRule(Long roomId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM booking_rule WHERE room_id = ?", roomId);
        if (list.isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO booking_rule(room_id, max_bookings_per_day, max_hours_per_booking, checkin_grace_minutes, temp_leave_minutes) " +
                    "VALUES (?, 2, 4, 120, 15)",
                roomId
            );
            return jdbcTemplate.queryForMap("SELECT * FROM booking_rule WHERE room_id = ?", roomId);
        }
        return list.get(0);
    }

    public void saveBookingRule(Long roomId, Integer maxPerDay, Integer maxHours, Integer grace, Integer leaveMinutes) {
        int n = jdbcTemplate.update(
            "UPDATE booking_rule SET max_bookings_per_day=?, max_hours_per_booking=?, checkin_grace_minutes=?, temp_leave_minutes=? WHERE room_id=?",
            maxPerDay, maxHours, grace, leaveMinutes, roomId
        );
        if (n == 0) {
            jdbcTemplate.update(
                "INSERT INTO booking_rule(room_id, max_bookings_per_day, max_hours_per_booking, checkin_grace_minutes, temp_leave_minutes) VALUES (?, ?, ?, ?, ?)",
                roomId, maxPerDay, maxHours, grace, leaveMinutes
            );
        }
    }

    public List<Map<String, Object>> reports() {
        return jdbcTemplate.queryForList(
            "SELECT vr.*, su.phone reporter_phone FROM violation_report vr " +
                "LEFT JOIN sys_user su ON su.id = vr.reporter_user_id ORDER BY vr.id DESC"
        );
    }

    public Map<String, Object> reportsPaged(int page, int pageSize) {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM violation_report", Integer.class);
        int p = Math.max(page, 1);
        int size = Math.max(pageSize, 1);
        int offset = (p - 1) * size;
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT vr.*, su.phone reporter_phone FROM violation_report vr " +
                "LEFT JOIN sys_user su ON su.id = vr.reporter_user_id ORDER BY vr.id DESC LIMIT ? OFFSET ?",
            size, offset
        );
        return pageResult(items, total == null ? 0 : total, p, size);
    }

    public List<Map<String, Object>> listUsers() {
        return jdbcTemplate.queryForList(
            "SELECT u.id, u.phone, u.role, u.status, u.violation_count, u.booking_restricted_until, u.created_at, " +
                "COALESCE(p.nickname, '') nickname " +
                "FROM sys_user u LEFT JOIN user_profile p ON p.user_id = u.id ORDER BY u.id DESC"
        );
    }

    public Map<String, Object> listUsersPaged(int page, int pageSize) {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
        int p = Math.max(page, 1);
        int size = Math.max(pageSize, 1);
        int offset = (p - 1) * size;
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT u.id, u.phone, u.role, u.status, u.violation_count, u.booking_restricted_until, u.created_at, " +
                "COALESCE(p.nickname, '') nickname " +
                "FROM sys_user u LEFT JOIN user_profile p ON p.user_id = u.id ORDER BY u.id DESC LIMIT ? OFFSET ?",
            size, offset
        );
        return pageResult(items, total == null ? 0 : total, p, size);
    }

    public void updateUserStatus(Long userId, Integer status) {
        jdbcTemplate.update("UPDATE sys_user SET status = ? WHERE id = ?", status, userId);
    }

    public void updateUser(Long userId, String phone, String nickname, Integer role, Integer status, Integer violationCount) {
        jdbcTemplate.update(
            "UPDATE sys_user SET phone = ?, role = ?, status = ?, violation_count = ? WHERE id = ?",
            phone, role, status, violationCount, userId
        );
        jdbcTemplate.update(
            "UPDATE user_profile SET nickname = ? WHERE user_id = ?",
            nickname, userId
        );
    }

    public void resetUserViolations(Long userId) {
        jdbcTemplate.update(
            "UPDATE sys_user SET violation_count = 0, booking_restricted_until = NULL WHERE id = ?",
            userId
        );
    }

    @Transactional
    public void handleReport(Long adminId, Long reportId, Integer status, String note) {
        Map<String, Object> report = jdbcTemplate.queryForMap("SELECT * FROM violation_report WHERE id = ?", reportId);
        int currentStatus = ((Number) report.get("status")).intValue();
        if (currentStatus != 0) {
            throw new BizException("该举报已处理，请刷新列表");
        }
        if (status == 1) {
            Object orderId = report.get("order_id");
            if (orderId != null) {
                Map<String, Object> order = jdbcTemplate.queryForMap(
                    "SELECT id, user_id, seat_id, booking_date, slot_code, status FROM booking_order WHERE id = ?",
                    orderId
                );
                int reportType = ((Number) report.getOrDefault("report_type", 2)).intValue();
                recordViolation(((Number) order.get("user_id")).longValue(), ((Number) order.get("id")).longValue(), reportType, "举报核实属实");
            }
        }
        jdbcTemplate.update(
            "UPDATE violation_report SET status=?, handle_admin_id=?, handle_note=?, handled_at=NOW() WHERE id=?",
            status, adminId, note, reportId
        );
    }

    public Map<String, Object> dashboard(LocalDate from, LocalDate to) {
        Map<String, Object> data = new HashMap<>();
        Integer totalBookings = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM booking_order WHERE booking_date BETWEEN ? AND ?", Integer.class, from, to);
        Integer completedBookings = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM booking_order WHERE booking_date BETWEEN ? AND ? AND status = 2", Integer.class, from, to);
        Integer violations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM violation_record WHERE DATE(created_at) BETWEEN ? AND ?", Integer.class, from, to);

        List<Map<String, Object>> hotSeatType = jdbcTemplate.queryForList(
            "SELECT s.seat_type, COUNT(*) c FROM booking_order bo JOIN seat s ON bo.seat_id = s.id " +
                "WHERE bo.booking_date BETWEEN ? AND ? GROUP BY s.seat_type ORDER BY c DESC", from, to
        );
        List<Map<String, Object>> monthlyTrend = jdbcTemplate.queryForList(
            "SELECT DATE_FORMAT(booking_date, '%Y-%m') ym, COUNT(*) total FROM booking_order " +
                "GROUP BY DATE_FORMAT(booking_date, '%Y-%m') ORDER BY ym DESC LIMIT 12"
        );
        data.put("totalBookings", totalBookings == null ? 0 : totalBookings);
        data.put("completedBookings", completedBookings == null ? 0 : completedBookings);
        data.put("violations", violations == null ? 0 : violations);
        data.put("hotSeatType", hotSeatType);
        data.put("monthlyTrend", monthlyTrend);
        return data;
    }

    public void assertAdmin(Long userId) {
        Integer role = jdbcTemplate.queryForObject("SELECT role FROM sys_user WHERE id = ?", Integer.class, userId);
        if (role == null || role != 9) {
            throw new BizException("管理员权限不足");
        }
    }

    private void recordViolation(Long userId, Long orderId, int type, String remark) {
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM violation_record WHERE user_id = ? AND order_id = ? AND violation_type = ?",
            Integer.class,
            userId,
            orderId,
            type
        );
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update(
            "INSERT INTO violation_record(user_id, order_id, violation_type, remark) VALUES (?, ?, ?, ?)",
            userId, orderId, type, remark
        );
        jdbcTemplate.update("UPDATE sys_user SET violation_count = violation_count + 1 WHERE id = ?", userId);
        jdbcTemplate.update("UPDATE points_account SET points_balance = points_balance - 10 WHERE user_id = ?", userId);
        jdbcTemplate.update("INSERT INTO points_txn(user_id, change_amount, biz_type, biz_ref_id) VALUES (?, -10, 4, ?)", userId, orderId);
        Integer count = jdbcTemplate.queryForObject("SELECT violation_count FROM sys_user WHERE id = ?", Integer.class, userId);
        if (count != null && count >= 3) {
            jdbcTemplate.update("UPDATE sys_user SET booking_restricted_until = DATE_ADD(NOW(), INTERVAL 7 DAY) WHERE id = ?", userId);
        }
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> items, int total, int page, int pageSize) {
        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("total", total);
        res.put("page", page);
        res.put("pageSize", pageSize);
        return res;
    }
}
