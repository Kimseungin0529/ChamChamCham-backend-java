package com.chamchamcham.api;
import com.chamchamcham.api.auth.dto.AuthRequests;
import com.chamchamcham.api.member.dto.MemberRequests;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class ExperienceLevelValidationTest {
    private final jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    @Test @DisplayName("onboarding range error does not also report age error")
    void onboardingRangeErrorDoesNotAlsoReportAgeError() {
        var request = new AuthRequests.CompleteOnboardingRequest(null, null, LocalDate.now().minusYears(30), null,
            101, null, null, List.of(), null);
        var fields = validator.validate(request).stream().map(v -> v.getPropertyPath().toString())
            .filter(p -> p.startsWith("experienceLevel")).toList();
        assertThat(fields).containsExactly("experienceLevel");
    }
    @Test @DisplayName("profile range error does not also report age error")
    void profileRangeErrorDoesNotAlsoReportAgeError() {
        var request = new MemberRequests.UpdateMyProfileRequest(null, null, LocalDate.now().minusYears(30), null,
            101, null, null, List.of());
        var fields = validator.validate(request).stream().map(v -> v.getPropertyPath().toString())
            .filter(p -> p.startsWith("experienceLevel")).toList();
        assertThat(fields).containsExactly("experienceLevel");
    }
}
