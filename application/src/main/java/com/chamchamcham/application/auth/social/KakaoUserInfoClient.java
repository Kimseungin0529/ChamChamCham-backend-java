package com.chamchamcham.application.auth.social;

import org.springframework.lang.NonNull;

public interface KakaoUserInfoClient {
    @NonNull KakaoUserInfo fetch(@NonNull String accessToken);
}
