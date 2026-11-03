package com.chamchamcham.application.exception.business;

import com.chamchamcham.application.exception.ApplicationException;
import com.chamchamcham.application.exception.ErrorCode;
import org.springframework.lang.Nullable;
import java.util.Objects;

public class BusinessException extends ApplicationException {
    private final ErrorCode errorCode;
    private final Object detail;
    private final String customMessage;

    public BusinessException(ErrorCode errorCode) { this(errorCode, null, null); }

    public BusinessException(ErrorCode errorCode, @Nullable Object detail) { this(errorCode, detail, null); }

    public BusinessException(ErrorCode errorCode, @Nullable Object detail, @Nullable String customMessage) {
        this(errorCode, detail, customMessage, customMessage != null ? customMessage : errorCode.getMessageKey());
    }

    public BusinessException(ErrorCode errorCode, @Nullable Object detail, @Nullable String customMessage, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.detail = detail;
        this.customMessage = customMessage;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    @Nullable
    public Object getDetail() { return detail; }
    @Nullable
    public String getCustomMessage() { return customMessage; }
}
