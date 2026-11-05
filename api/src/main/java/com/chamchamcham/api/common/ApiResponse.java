package com.chamchamcham.api.common;

import kotlin.Unit;
import org.springframework.lang.Nullable;

public record ApiResponse<T>(boolean success, @Nullable T data, @Nullable ApiError error) {
    public boolean getSuccess() { return success; }
    @Nullable public T getData() { return data; }
    @Nullable public ApiError getError() { return error; }

    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null); }
    public static <T> ApiResponse<T> empty(T data) { return new ApiResponse<>(true, null, null); }
    public static ApiResponse<Unit> fail(String code, String messageKey) { return fail(code, messageKey, null); }
    public static ApiResponse<Unit> fail(String code, String messageKey, @Nullable Object detail) {
        return new ApiResponse<>(false, null, new ApiError(code, messageKey, detail));
    }
}
