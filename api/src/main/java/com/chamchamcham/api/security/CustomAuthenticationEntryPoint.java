package com.chamchamcham.api.security;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
        throws IOException {
        var body = ApiResponse.fail(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessageKey(),
            "Authorization header missing or invalid");
        response.setStatus(ErrorCode.UNAUTHORIZED.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(toJson(body));
    }

    static String toJson(ApiResponse<?> body) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(body);
    }
}
