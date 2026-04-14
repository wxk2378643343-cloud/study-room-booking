package com.studyhub.config;

import com.studyhub.auth.TokenService;
import com.studyhub.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final JdbcTemplate jdbcTemplate;

    public AuthInterceptor(TokenService tokenService, JdbcTemplate jdbcTemplate) {
        this.tokenService = tokenService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/health") || path.startsWith("/api/v1/auth/") || path.startsWith("/uploads/")) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException("unauthorized");
        }
        String token = auth.substring(7);
        Long userId = tokenService.verifyAndGetUserId(token);
        Integer status = jdbcTemplate.queryForObject("SELECT status FROM sys_user WHERE id = ?", Integer.class, userId);
        if (status == null || status != 1) {
            throw new BizException("用户已禁用");
        }
        request.setAttribute("uid", userId);
        return true;
    }
}
