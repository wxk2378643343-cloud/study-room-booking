package com.studyhub.auth;

import com.studyhub.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {
    public Long currentUserId(HttpServletRequest request) {
        Object uid = request.getAttribute("uid");
        if (uid == null) {
            throw new BizException("unauthorized");
        }
        return (Long) uid;
    }
}
