package com.chamchamcham.application.exception;

import org.springframework.lang.NonNull;
import java.util.Objects;

public abstract class ApplicationException extends RuntimeException {
    public ApplicationException(String message) {
        super(Objects.requireNonNull(message, "message"));
    }

    @Override
    @NonNull
    public String getMessage() { return super.getMessage(); }
}
