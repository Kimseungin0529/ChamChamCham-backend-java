package com.chamchamcham.api.exception;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.common.LoggingUtil;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import jakarta.validation.ConstraintViolationException;
import kotlin.Unit;
import mu.KLogger;
import mu.KotlinLogging;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Map;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final KLogger logger = KotlinLogging.INSTANCE.logger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleBusiness(BusinessException e) {
        var body = ApiResponse.fail(e.getErrorCode().getCode(), e.getCustomMessage() != null ? e.getCustomMessage() : e.getErrorCode().getMessageKey(), e.getDetail());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        LoggingUtil.logBusinessError(logger, e);
        var fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        var detail = fieldError == null ? null : fieldDetail(fieldError.getField(),
            fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value");
        var body = ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), ErrorCode.INVALID_INPUT.getMessageKey(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleBindException(BindException e) {
        LoggingUtil.logBusinessError(logger, e);
        var fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        var detail = fieldError == null ? null : fieldDetail(fieldError.getField(),
            fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value");
        var body = ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), ErrorCode.INVALID_INPUT.getMessageKey(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleConstraintViolation(ConstraintViolationException e) {
        LoggingUtil.logBusinessError(logger, e);
        var violation = e.getConstraintViolations().stream().findFirst().orElse(null);
        Map<String, String> detail = null;
        if (violation != null) {
            var path = violation.getPropertyPath().toString();
            detail = fieldDetail(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        }
        var body = ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), ErrorCode.INVALID_INPUT.getMessageKey(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        LoggingUtil.logBusinessError(logger, e);
        var body = ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), ErrorCode.INVALID_INPUT.getMessageKey(),
            fieldDetail(e.getName(), e.getValue() != null ? "Invalid value: " + e.getValue() : "Invalid value"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        LoggingUtil.logBusinessError(logger, e);
        var body = ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), ErrorCode.INVALID_INPUT.getMessageKey(),
            fieldDetail(e.getParameterName(), "Required parameter is missing"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleJsonParse(HttpMessageNotReadableException e) {
        var eventId = LoggingUtil.logBusinessError(logger, e);
        var detail = new LinkedHashMap<String, String>();
        detail.put("reason", "Invalid JSON format");
        detail.put("eventId", eventId);
        var body = ApiResponse.fail(ErrorCode.INVALID_JSON.getCode(), ErrorCode.INVALID_JSON.getMessageKey(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Unit>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
            ErrorCode.RESOURCE_NOT_FOUND.getMessageKey(), e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Unit>> handleException(Exception e) {
        var eventId = LoggingUtil.logUnexpectedError(logger, e);
        var body = ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessageKey(), Map.of("eventId", eventId));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
    private static Map<String, String> fieldDetail(String field, String reason) {
        var detail = new LinkedHashMap<String, String>();
        detail.put("field", field);
        detail.put("reason", reason);
        return detail;
    }
}
