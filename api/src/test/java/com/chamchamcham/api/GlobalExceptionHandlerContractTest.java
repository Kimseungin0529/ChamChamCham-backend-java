package com.chamchamcham.api;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.api.farm.dto.FarmRequests;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerContractTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void businessFailureKeepsStatusCustomMessageAndDetail() {
        var response = handler.handleBusiness(new BusinessException(ErrorCode.FARM_IN_USE, Map.of("farmId", "fixture"), "custom.message"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("FARM_002");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("custom.message");
        assertThat(response.getBody().getError().getDetail()).isEqualTo(Map.of("farmId", "fixture"));
        assertThat(handler.handleBusiness(new BusinessException(ErrorCode.FARM_IN_USE)).getBody().getError().getMessage())
            .isEqualTo("error.farm_in_use");
    }

    @SuppressWarnings("unchecked")
    @Test void bindingErrorsKeepOnlyFirstFieldAndFallbackReason() throws Exception {
        var result = new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError("request", "first", null));
        result.addError(new FieldError("request", "second", "second reason"));
        var bound = handler.handleBindException(new BindException(result));
        var validated = handler.handleMethodArgumentNotValid(new MethodArgumentNotValidException(parameter(), result));
        for (var response : java.util.List.of(bound, validated)) {
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_001");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("error.invalid_input");
            assertThat(response.getBody().getError().getDetail()).isEqualTo(Map.of("field", "first", "reason", "Invalid value"));
            var detail = (Map<String, String>) response.getBody().getError().getDetail();
            assertThat(detail.keySet()).containsExactly("field", "reason");
            detail.put("field", "updated");
            assertThat(detail.get("field")).isEqualTo("updated");
        }
    }

    @Test void constraintFailureKeepsPropertyNameAndValidationMessage() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new FarmRequests.BoundaryCoordinateRequest(null, 128.5));
            var response = handler.handleConstraintViolation(new ConstraintViolationException(violations));
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_001");
            assertThat(response.getBody().getError().getDetail()).isEqualTo(Map.of("field", "latitude", "reason", "경계 위도를 입력해주세요"));
        }
    }

    @Test void conversionAndMissingParameterFailuresKeepLiteralDetails() throws Exception {
        for (Object value : new Object[]{"bad", null}) {
            var response = handler.handleMethodArgumentTypeMismatch(new MethodArgumentTypeMismatchException(value, Integer.class, "count", parameter(), new IllegalArgumentException()));
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_001");
            assertThat(response.getBody().getError().getDetail()).isEqualTo(Map.of("field", "count", "reason", value == null ? "Invalid value" : "Invalid value: bad"));
        }
        var missing = handler.handleMissingServletRequestParameter(new MissingServletRequestParameterException("count", "Integer"));
        assertThat(missing.getStatusCode().value()).isEqualTo(400);
        assertThat(missing.getBody().getError().getCode()).isEqualTo("COMMON_001");
        assertThat(missing.getBody().getError().getDetail()).isEqualTo(Map.of("field", "count", "reason", "Required parameter is missing"));
    }

    @Test void malformedJsonRetainsEventIdAndSafeReason() {
        MDC.put("eventId", "fixture-event");
        try {
            var response = handler.handleJsonParse(new HttpMessageNotReadableException("private parse detail", new MockHttpInputMessage(new byte[0])));
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_002");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("error.invalid_json");
            assertThat(response.getBody().getError().getDetail()).isEqualTo(Map.of("reason", "Invalid JSON format", "eventId", "fixture-event"));
        } finally { MDC.remove("eventId"); }
    }

    @Test void unexpectedFailureExposesOnlyEventId() {
        MDC.put("eventId", "fixture-event");
        try {
            var response = handler.handleException(new IllegalStateException("internal fixture failure"));
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_999");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("error.internal_error");
            assertThat(response.getBody().getError().getDetail()).isEqualTo(Map.of("eventId", "fixture-event"));
        } finally { MDC.remove("eventId"); }
    }

    @Test void resourceNotFoundKeepsExistingResourceMessage() {
        var response = handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "missing"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_001");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("error.resource_not_found");
        assertThat(response.getBody().getError().getDetail()).isEqualTo("No static resource missing.");
    }

    public void bindingTarget(String value) {}
    private MethodParameter parameter() throws Exception {
        return new MethodParameter(getClass().getMethod("bindingTarget", String.class), 0);
    }
}
