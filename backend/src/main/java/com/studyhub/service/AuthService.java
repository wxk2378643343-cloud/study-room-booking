package com.studyhub.service;

import com.studyhub.auth.TokenService;
import com.studyhub.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final JdbcTemplate jdbcTemplate;
    private final TokenService tokenService;

    public AuthService(JdbcTemplate jdbcTemplate, TokenService tokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenService = tokenService;
    }

    public Map<String, Object> sendCode(String phone) {
        Map<String, Object> result = new HashMap<>();
        result.put("phone", phone);
        result.put("code", "123456");
        result.put("expiredAt", LocalDateTime.now().plusMinutes(5).toString());
        return result;
    }

    public Map<String, Object> register(String phone, String code, String password, String nickname) {
        if (!"123456".equals(code)) {
            throw new BizException("验证码错误");
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE phone = ?", Integer.class, phone);
        if (count != null && count > 0) {
            throw new BizException("手机号已注册");
        }
        jdbcTemplate.update("INSERT INTO sys_user(phone, password_hash, role, status) VALUES (?, ?, 1, 1)", phone, sha256(password));
        Long uid = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE phone = ?", Long.class, phone);
        jdbcTemplate.update("INSERT INTO user_profile(user_id, nickname) VALUES (?, ?)", uid, nickname);
        jdbcTemplate.update("INSERT INTO points_account(user_id, points_balance) VALUES (?, 0)", uid);
        String token = tokenService.generateToken(uid);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", uid);
        result.put("nickname", nickname);
        return result;
    }

    public Map<String, Object> login(String phone, String password) {
        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap("SELECT id, password_hash, role FROM sys_user WHERE phone = ? AND status = 1", phone);
        } catch (Exception e) {
            throw new BizException("用户不存在或已禁用");
        }
        String stored = String.valueOf(row.get("password_hash"));
        if (!stored.equals(sha256(password))) {
            throw new BizException("密码错误");
        }
        Long uid = ((Number) row.get("id")).longValue();
        String nickname = jdbcTemplate.queryForObject("SELECT nickname FROM user_profile WHERE user_id = ?", String.class, uid);
        String token = tokenService.generateToken(uid);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", uid);
        result.put("nickname", nickname);
        result.put("role", row.get("role"));
        return result;
    }

    public Map<String, Object> me(Long userId) {
        return jdbcTemplate.queryForMap(
            "SELECT u.id, u.phone, u.role, u.violation_count, u.booking_restricted_until, p.nickname, p.avatar_url " +
                "FROM sys_user u LEFT JOIN user_profile p ON u.id = p.user_id WHERE u.id = ?",
            userId
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
