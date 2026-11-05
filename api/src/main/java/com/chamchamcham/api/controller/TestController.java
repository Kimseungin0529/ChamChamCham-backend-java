package com.chamchamcham.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {
    @GetMapping("/ping")
    public String ping() { return "pong"; }

    @GetMapping("/me")
    public String getMyInfo(@AuthenticationPrincipal String memberId) {
        java.util.Objects.requireNonNull(memberId, "memberId");
        return "인증 성공, 현재 로그인한 회원의 ID(PK)는: " + memberId;
    }
}
