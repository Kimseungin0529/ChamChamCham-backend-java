package com.chamchamcham.application.auth.local;

import org.springframework.lang.NonNull;

public interface EmailSender {
    void sendVerificationCode(@NonNull String email, @NonNull String code);
}
