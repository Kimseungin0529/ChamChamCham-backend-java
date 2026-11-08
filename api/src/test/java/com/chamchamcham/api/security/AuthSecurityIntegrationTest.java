package com.chamchamcham.api.security;

import static com.chamchamcham.api.testsupport.TestJwtFixtures.signedTestToken;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;


@SpringBootTest(
    properties = {
        "spring.mail.username=test@example.com",
        "spring.mail.password=test-password"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuthSecurityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private TokenProvider tokenProvider;
    @Autowired private MemberRepository memberRepository;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Test
    @DisplayName("auth endpoints are publicly accessible")
    void authEndpointsArePubliclyAccessible() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"password\":\"password123\"}")
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("kakao auth endpoint is publicly accessible")
    void kakaoAuthEndpointIsPubliclyAccessible() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/kakao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"\",\"nonce\":\"nonce\"}")
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("apple auth endpoint is publicly accessible")
    void appleAuthEndpointIsPubliclyAccessible() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/apple/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identityToken\":\"\",\"nonce\":\"nonce\"}")
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("naver auth endpoint is publicly accessible")
    void naverAuthEndpointIsPubliclyAccessible() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accessToken\":\"\"}")
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("onboarding complete requires authentication")
    void onboardingCompleteRequiresAuthentication() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\n  \"name\":\"홍길동\",\n  \"phone\":\"010-1234-5678\",\n  \"birthDate\":\"1990-01-01\",\n  \"nickname\":\"길동\",\n  \"experienceLevel\":3,\n  \"managementType\":\"AGRICULTURAL_INDIVIDUAL\",\n  \"farm\": {\n    \"name\":\"길동농장\",\n    \"roadAddress\":\"서울시 강남구 테헤란로 1\",\n    \"latitude\":35.8465,\n    \"longitude\":127.1292\n  },\n  \"cropIds\":[\"00000000-0000-0000-0000-000000000101\"]\n}"
                )
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("oauth redirect routes are not public auth routes")
    void oauthRedirectRoutesAreNotPublicAuthRoutes() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/kakao"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected endpoint rejects missing token")
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/test/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("cors preflight allows configured origin")
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(
            options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("cors preflight rejects unconfigured origin")
    void corsPreflightRejectsUnconfiguredOrigin() throws Exception {
        mockMvc.perform(
            options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        )
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("protected api denial is logged with final status guest member and forwarded client ip")
    void protectedApiDenialIsLoggedWithFinalStatusGuestMemberAndForwardedClientIp(CapturedOutput output) throws Exception {
        mockMvc.perform(
            get("/api/v1/test/me")
                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.2")
        )
            .andExpect(status().isUnauthorized());

        var logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("[REQ START]")
            .contains("[REQ END]")
            .contains("status=401")
            .contains("WARN")
            .contains("client=203.0.113.7")
            .contains("[memberId=GUEST]")
            .contains("[ip=203.0.113.7]");

        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("eventId")).isNull();
        assertThat(MDC.get("memberId")).isNull();
        assertThat(MDC.get("clientIp")).isNull();
    }

    @Test
    @DisplayName("protected api denial uses proxy client ip when forwarded header is absent")
    void protectedApiDenialUsesProxyClientIpWhenForwardedHeaderIsAbsent(CapturedOutput output) throws Exception {
        mockMvc.perform(
            get("/api/v1/test/me")
                .header("X-Forwarded-For", "unknown")
                .header("Proxy-Client-IP", "198.51.100.9")
        )
            .andExpect(status().isUnauthorized());

        var logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("[REQ START]")
            .contains("[REQ END]")
            .contains("status=401")
            .contains("client=198.51.100.9")
            .contains("[ip=198.51.100.9]");
    }

    @Test
    @DisplayName("public remote address ignores spoofed forwarded client ip")
    void publicRemoteAddressIgnoresSpoofedForwardedClientIp(CapturedOutput output) throws Exception {
        mockMvc.perform(
            get("/api/v1/test/me")
                .with(request -> { request.setRemoteAddr("198.51.100.10"); return request; })
                .header("X-Forwarded-For", "203.0.113.7")
        )
            .andExpect(status().isUnauthorized());

        var logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("[REQ START]")
            .contains("[REQ END]")
            .contains("status=401")
            .contains("client=198.51.100.10")
            .contains("[ip=198.51.100.10]")
            .doesNotContain("client=203.0.113.7")
            .doesNotContain("[ip=203.0.113.7]");
    }

    @Test
    @DisplayName("protected endpoint accepts valid jwt")
    void protectedEndpointAcceptsValidJwt() throws Exception {
        var existingMember = memberRepository.saveAndFlush(
            new Member("security-" + (UUID.randomUUID()) + "@example.com", null)
        );
        var existingMemberId = java.util.Objects.requireNonNull(existingMember.getId());
        var accessToken = tokenProvider.createAccessToken(existingMemberId, "ROLE_USER");

        try {
            mockMvc.perform(
                get("/api/v1/test/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + (accessToken) + "")
            )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(existingMemberId.toString())));
        } finally {
            memberRepository.delete(existingMember);
        }
    }

    @Test
    @DisplayName("protected endpoint rejects signed access token with non uuid subject")
    void protectedEndpointRejectsSignedAccessTokenWithNonUuidSubject() throws Exception {
        var accessToken = signedTestToken("42", "access", "ROLE_USER");

        mockMvc.perform(
            get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + (accessToken) + "")
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh token cannot authenticate protected endpoint")
    void refreshTokenCannotAuthenticateProtectedEndpoint() throws Exception {
        var refreshToken = tokenProvider.createRefreshToken(memberId);

        mockMvc.perform(
            get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + (refreshToken) + "")
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout requires authentication")
    void logoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("entry point preserves explicit null data error detail and UTF8 JSON")
    void entryPointPreservesJsonContract() throws Exception {
        var response = mockMvc.perform(get("/api/v1/test/me")).andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error.code").value("AUTH_001"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error.detail").value("Authorization header missing or invalid"))
            .andReturn().getResponse();
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        assertThat(tree.has("data")).isTrue();
        assertThat(tree.get("data").isNull()).isTrue();
    }

}
