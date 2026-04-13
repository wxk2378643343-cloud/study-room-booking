package com.studyhub.auth;

import com.studyhub.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {

    @Value("${app.auth.secret:studyhub-secret}")
    private String secret;

    @Value("${app.auth.ttl-seconds:259200}")
    private long ttlSeconds;

    public String generateToken(Long userId) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = userId + ":" + exp;
        String sign = sha256(payload + ":" + secret);
        String raw = payload + ":" + sign;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Long verifyAndGetUserId(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 3) {
                throw new BizException("invalid token");
            }
            long userId = Long.parseLong(parts[0]);
            long exp = Long.parseLong(parts[1]);
            String sign = parts[2];
            String payload = parts[0] + ":" + parts[1];
            if (!sha256(payload + ":" + secret).equals(sign)) {
                throw new BizException("invalid token");
            }
            if (Instant.now().getEpochSecond() > exp) {
                throw new BizException("token expired");
            }
            return userId;
        } catch (IllegalArgumentException e) {
            throw new BizException("invalid token");
        }
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
