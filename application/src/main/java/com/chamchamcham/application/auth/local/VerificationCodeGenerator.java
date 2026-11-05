package com.chamchamcham.application.auth.local;

import org.springframework.lang.NonNull;

public interface VerificationCodeGenerator {
    @NonNull String generate();
}
