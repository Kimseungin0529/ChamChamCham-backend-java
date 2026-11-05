package com.chamchamcham.api.common;

import java.util.Objects;
import org.springframework.lang.Nullable;

public record ApiError(String code, String message, @Nullable Object detail) {
    public ApiError { Objects.requireNonNull(code, "code"); Objects.requireNonNull(message, "message"); }
    public ApiError(String code, String message) { this(code, message, null); }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    @Nullable public Object getDetail() { return detail; }
}
