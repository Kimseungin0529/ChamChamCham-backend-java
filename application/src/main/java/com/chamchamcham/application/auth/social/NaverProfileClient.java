package com.chamchamcham.application.auth.social;

import org.springframework.lang.NonNull;

public interface NaverProfileClient {
    @NonNull NaverProfile fetch(@NonNull String accessToken);
}
