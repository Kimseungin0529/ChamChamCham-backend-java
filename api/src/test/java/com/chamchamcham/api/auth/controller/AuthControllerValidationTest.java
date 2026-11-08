package com.chamchamcham.api.auth.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.auth.common.OnboardingService;
import com.chamchamcham.application.auth.local.AuthService;
import com.chamchamcham.application.auth.social.AppleLoginService;
import com.chamchamcham.application.auth.social.KakaoLoginService;
import com.chamchamcham.application.auth.social.NaverLoginService;
import com.chamchamcham.application.security.TokenProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerValidationTest {
    @Autowired private MockMvc mockMvc;
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @MockBean
    private AuthService authService;

    @MockBean
    private KakaoLoginService kakaoLoginService;

    @MockBean
    private AppleLoginService appleLoginService;

    @MockBean
    private NaverLoginService naverLoginService;

    @MockBean
    private OnboardingService onboardingService;

    @MockBean
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("send-code rejects invalid email")
    void sendCodeRejectsInvalidEmail() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/email/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("signup rejects short password without echoing rejected value")
    void signupRejectsShortPasswordWithoutEchoingRejectedValue(CapturedOutput output) throws Exception {
        var result = mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"123\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("password")))
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("123");
        assertThat(output.getOut() + output.getErr())
            .doesNotContain("\"password\":\"123\"")
            .doesNotContain("password=123");
    }

    @Test
    @DisplayName("kakao login rejects missing id token")
    void kakaoLoginRejectsMissingIdToken() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/kakao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"\",\"nonce\":\"nonce\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("kakao login accepts missing access token")
    void kakaoLoginAcceptsMissingAccessToken() throws Exception {
        when(kakaoLoginService.login(new AuthCommand.KakaoLogin("id-token", "nonce", null)))
            .thenReturn(loginResult());

        mockMvc.perform(
            post("/api/v1/auth/kakao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"id-token\",\"nonce\":\"nonce\"}")
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("apple login rejects missing identity token")
    void appleLoginRejectsMissingIdentityToken() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/apple/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identityToken\":\"\",\"nonce\":\"nonce\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("naver login rejects missing access token")
    void naverLoginRejectsMissingAccessToken() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accessToken\":\"\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("complete onboarding rejects more than five crop ids")
    void completeOnboardingRejectsMoreThanFiveCropIds() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody(3, "AGRICULTURAL_INDIVIDUAL", "[\"" + (cropId) + "\",\"" + (UUID.randomUUID()) + "\",\"" + (UUID.randomUUID()) + "\",\"" + (UUID.randomUUID()) + "\",\"" + (UUID.randomUUID()) + "\",\"" + (UUID.randomUUID()) + "\"]"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("cropIds")));
    }

    @Test
    @DisplayName("complete onboarding rejects duplicate crop ids")
    void completeOnboardingRejectsDuplicateCropIds() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody(3, "AGRICULTURAL_INDIVIDUAL", "[\"" + (cropId) + "\",\"" + (cropId) + "\"]"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("cropIds")));
    }

    @Test
    @DisplayName("complete onboarding rejects experience level below zero")
    void completeOnboardingRejectsExperienceLevelBelowZero() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody(-1, "AGRICULTURAL_INDIVIDUAL", "[\"" + cropId + "\"]"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("experienceLevel")));
    }

    @Test
    @DisplayName("complete onboarding rejects experience level above one hundred")
    void completeOnboardingRejectsExperienceLevelAboveOneHundred() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody(101, "AGRICULTURAL_INDIVIDUAL", "[\"" + cropId + "\"]"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("experienceLevel")));
    }

    @Test
    @DisplayName("complete onboarding rejects empty crop ids")
    void completeOnboardingRejectsEmptyCropIds() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody(3, "AGRICULTURAL_INDIVIDUAL", "[]"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("cropIds")));
    }

    @Test
    @DisplayName("complete onboarding rejects invalid management type")
    void completeOnboardingRejectsInvalidManagementType() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody(3, "INVALID_TYPE", "[\"" + cropId + "\"]"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_002")));
    }

    private AuthResult.Login loginResult() {
        return new AuthResult.Login(
            "access-token",
            "refresh-token",
            new AuthResult.MemberProfile(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            new AuthResult.Onboarding(
                AuthResult.OnboardingStatus.REQUIRED,
                java.util.List.of()
            )
        );
    }

    private String validOnboardingRequestBody(int experienceLevel, String managementType, String cropIdsJson) {
        return "{\n  \"name\":\"홍길동\",\n  \"phone\":\"010-1234-5678\",\n  \"birthDate\":\"1990-01-01\",\n  \"nickname\":\"길동\",\n  \"experienceLevel\":" + (experienceLevel) + ",\n  \"managementType\":\"" + (managementType) + "\",\n  \"farm\": {\n    \"name\":\"길동농장\",\n    \"roadAddress\":\"서울시 강남구 테헤란로 1\",\n    \"jibunAddress\":\"서울시 강남구 역삼동 1\",\n    \"latitude\":35.8465,\n    \"longitude\":127.1292,\n    \"pnu\":\"4511310200101230004\",\n    \"landCategory\":\"전\",\n    \"areaSqm\":1200.5,\n    \"areaIsManualEntry\":false,\n    \"boundaryCoordinates\":[\n      {\"latitude\":35.8461,\"longitude\":127.1289}\n    ],\n    \"dataSource\":{\n      \"address\":\"JUSO\",\n      \"coordinate\":\"V_WORLD_ADDRESS\",\n      \"parcel\":\"V_WORLD_CADASTRAL\",\n      \"landCharacteristic\":\"V_WORLD_LAND_CHARACTERISTIC\"\n    }\n  },\n  \"cropIds\":" + (cropIdsJson) + "\n}";
    }

    @Test
    @DisplayName("missing and null required strings retain JSON parse error boundary")
    void missingAndNullRequiredStringsRemainJsonParseErrors() throws Exception {
        for (String body : java.util.List.of("{}", "{\"email\":null}", "{\"email\":\"valid@example.test\"}")) {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("COMMON_002"));
        }
    }

    @Test
    @DisplayName("explicit null optional provider token retains default null service command")
    void explicitNullOptionalProviderTokenIsAccepted() throws Exception {
        when(kakaoLoginService.login(new AuthCommand.KakaoLogin("id-token", "nonce", null))).thenReturn(loginResult());
        mockMvc.perform(post("/api/v1/auth/kakao/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"idToken\":\"id-token\",\"nonce\":\"nonce\",\"kakaoAccessToken\":null}"))
            .andExpect(status().isOk());
        org.mockito.Mockito.verify(kakaoLoginService).login(new AuthCommand.KakaoLogin("id-token", "nonce", null));
    }

}
