package com.studyhub.service;

import com.studyhub.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> profile(Long userId) {
        return jdbcTemplate.queryForMap(
            "SELECT u.id, u.phone, u.role, u.violation_count, u.booking_restricted_until, " +
                "p.nickname, p.avatar_url, p.preferred_room_id FROM sys_user u " +
                "LEFT JOIN user_profile p ON p.user_id = u.id WHERE u.id = ?",
            userId
        );
    }

    public void updateProfile(Long userId, String nickname, Long preferredRoomId) {
        jdbcTemplate.update("UPDATE user_profile SET nickname = ?, preferred_room_id = ? WHERE user_id = ?",
            nickname, preferredRoomId, userId);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        String stored = jdbcTemplate.queryForObject("SELECT password_hash FROM sys_user WHERE id = ?", String.class, userId);
        if (stored == null || !stored.equals(sha256(oldPassword))) {
            throw new BizException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码至少6位");
        }
        jdbcTemplate.update("UPDATE sys_user SET password_hash = ? WHERE id = ?", sha256(newPassword), userId);
    }

    public List<Map<String, Object>> violations(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM violation_record WHERE user_id = ? ORDER BY id DESC", userId);
    }

    public Map<String, Object> points(Long userId) {
        return jdbcTemplate.queryForMap("SELECT points_balance FROM points_account WHERE user_id = ?", userId);
    }

    public List<Map<String, Object>> pointLogs(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM points_txn WHERE user_id = ? ORDER BY id DESC LIMIT 100", userId);
    }

    public List<Map<String, Object>> coupons(Long userId) {
        return jdbcTemplate.queryForList(
            "SELECT uc.*, ct.name template_name, ct.points_cost FROM user_coupon uc " +
                "JOIN coupon_template ct ON uc.template_id = ct.id WHERE uc.user_id = ? ORDER BY uc.id DESC",
            userId
        );
    }

    public List<Map<String, Object>> notifications(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM notification WHERE user_id = ? ORDER BY id DESC LIMIT 100", userId);
    }

    public Integer unreadCount(Long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0", Integer.class, userId);
    }

    public void readNotification(Long userId, Long id) {
        jdbcTemplate.update("UPDATE notification SET is_read = 1 WHERE id = ? AND user_id = ?", id, userId);
    }

    public void createReport(Long userId, Long orderId, Long seatId, String imageUrl, String description) {
        jdbcTemplate.update(
            "INSERT INTO violation_report(reporter_user_id, order_id, seat_id, image_url, description_text, status) VALUES (?, ?, ?, ?, ?, 0)",
            userId, orderId, seatId, imageUrl, description
        );
    }

    private String sha256(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BizException("hash error");
        }
    }
}
