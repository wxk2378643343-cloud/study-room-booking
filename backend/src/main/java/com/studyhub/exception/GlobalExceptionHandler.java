package com.studyhub.exception;

import com.studyhub.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex, HttpServletResponse response) {
        response.setStatus(400);
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ApiResponse<Void> handleEmpty(EmptyResultDataAccessException ex, HttpServletResponse response) {
        response.setStatus(400);
        return ApiResponse.fail("记录不存在或无权限");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleEx(Exception ex, HttpServletResponse response) {
        response.setStatus(500);
        return ApiResponse.fail("server error: " + ex.getMessage());
    }
}
