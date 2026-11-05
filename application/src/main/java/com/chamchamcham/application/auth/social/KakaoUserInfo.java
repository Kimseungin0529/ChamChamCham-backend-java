package com.chamchamcham.application.auth.social;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record KakaoUserInfo(
    @NonNull String subject,
    @Nullable String email,
    @Nullable String name,
    @Nullable String phone,
    @Nullable LocalDate birthDate
) {
    public KakaoUserInfo {
        Objects.requireNonNull(subject, "subject");
    }

    @NonNull
    public String getSubject() { return subject; }
    @Nullable
    public String getEmail() { return email; }
    @Nullable
    public String getName() { return name; }
    @Nullable
    public String getPhone() { return phone; }
    @Nullable
    public LocalDate getBirthDate() { return birthDate; }
}
