package com.chamchamcham.api.security;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
        throws IOException {
        var body = ApiResponse.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessageKey(),
            "You do not have permission to access this resource");
        response.setStatus(ErrorCode.FORBIDDEN.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(CustomAuthenticationEntryPoint.toJson(body));
    }
}
