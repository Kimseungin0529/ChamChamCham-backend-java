package com.chamchamcham.api.auth.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.auth.common.OnboardingService;
import com.chamchamcham.application.auth.local.AuthService;
import com.chamchamcham.application.auth.social.AppleLoginService;
import com.chamchamcham.application.auth.social.KakaoLoginService;
import com.chamchamcham.application.auth.social.NaverLoginService;
import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farm.FarmCommand;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.ManagementType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {"app.auth.refresh-cookie-secure=true"})
class AuthControllerBusinessTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000011");
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
    @DisplayName("login returns token pair from service")
    void loginReturnsTokenPairFromService() throws Exception {
        when(authService.login(new AuthCommand.Login("user@example.com", "password123")))
            .thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", equalTo("access-token")))
            .andExpect(jsonPath("$.data.refreshToken", equalTo("refresh-token")));
    }

    @Test
    @DisplayName("send-code maps business exception to failure response")
    void sendCodeMapsBusinessExceptionToFailureResponse() throws Exception {
        doThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
            .when(authService)
            .sendVerificationCode(new AuthCommand.SendVerificationCode("taken@example.com"));

        mockMvc.perform(
            post("/api/v1/auth/email/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"taken@example.com\"}")
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_003")));
    }

    @Test
    @DisplayName("reissue returns rotated token pair and refresh cookie")
    void reissueReturnsRotatedTokenPairAndRefreshCookie() throws Exception {
        when(authService.reissue(new AuthCommand.Reissue("refresh-token")))
            .thenReturn(new AuthResult.TokenPair("new-access-token", "new-refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);

        mockMvc.perform(
            post("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-token\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken", equalTo("new-access-token")))
            .andExpect(jsonPath("$.data.refreshToken", equalTo("new-refresh-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=new-refresh-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Secure")));
    }

    @Test
    @DisplayName("kakao login returns login response from service")
    void kakaoLoginReturnsLoginResponseFromService() throws Exception {
        when(kakaoLoginService.login(new AuthCommand.KakaoLogin("id-token", "nonce", null)))
            .thenReturn(loginResult());

        mockMvc.perform(
            post("/api/v1/auth/kakao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"id-token\",\"nonce\":\"nonce\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", equalTo("access-token")))
            .andExpect(jsonPath("$.data.refreshToken", equalTo("refresh-token")))
            .andExpect(jsonPath("$.data.member.id", equalTo("00000000-0000-0000-0000-000000000001")))
            .andExpect(jsonPath("$.data.member.email", equalTo("social@example.com")))
            .andExpect(jsonPath("$.data.member.managementType").doesNotExist())
            .andExpect(jsonPath("$.data.onboarding.status", equalTo("REQUIRED")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[0]", equalTo("NAME")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[1]", equalTo("PHONE")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[2]", equalTo("BIRTH_DATE")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[3]", equalTo("NICKNAME")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[4]", equalTo("EXPERIENCE_LEVEL")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[5]", equalTo("MANAGEMENT_TYPE")));
    }

    @Test
    @DisplayName("kakao login passes optional access token to service")
    void kakaoLoginPassesOptionalAccessTokenToService() throws Exception {
        when(kakaoLoginService.login(new AuthCommand.KakaoLogin("id-token", "nonce", "kakao-access-token")))
            .thenReturn(loginResult());

        mockMvc.perform(
            post("/api/v1/auth/kakao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\n  \"idToken\":\"id-token\",\n  \"nonce\":\"nonce\",\n  \"kakaoAccessToken\":\"kakao-access-token\"\n}"
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", equalTo("access-token")));
    }

    @Test
    @DisplayName("apple login returns login response from service")
    void appleLoginReturnsLoginResponseFromService() throws Exception {
        when(
            appleLoginService.login(
                new AuthCommand.AppleLogin(
                    "identity-token",
                    "nonce",
                    "authorization-code",
                    "apple-user-id"
                )
            )
        ).thenReturn(loginResult());

        mockMvc.perform(
            post("/api/v1/auth/apple/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\n  \"identityToken\":\"identity-token\",\n  \"nonce\":\"nonce\",\n  \"authorizationCode\":\"authorization-code\",\n  \"userIdentifier\":\"apple-user-id\"\n}"
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", equalTo("access-token")))
            .andExpect(jsonPath("$.data.refreshToken", equalTo("refresh-token")))
            .andExpect(jsonPath("$.data.member.email", equalTo("social@example.com")))
            .andExpect(jsonPath("$.data.onboarding.status", equalTo("REQUIRED")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[0]", equalTo("NAME")));
    }

    @Test
    @DisplayName("naver login returns login response from service")
    void naverLoginReturnsLoginResponseFromService() throws Exception {
        when(naverLoginService.login(new AuthCommand.NaverLogin("naver-access-token")))
            .thenReturn(loginResult());

        mockMvc.perform(
            post("/api/v1/auth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accessToken\":\"naver-access-token\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", equalTo("access-token")))
            .andExpect(jsonPath("$.data.refreshToken", equalTo("refresh-token")))
            .andExpect(jsonPath("$.data.member.email", equalTo("social@example.com")))
            .andExpect(jsonPath("$.data.onboarding.status", equalTo("REQUIRED")))
            .andExpect(jsonPath("$.data.onboarding.missingFields[0]", equalTo("NAME")));
    }

    @Test
    @DisplayName("kakao login maps invalid token to unauthorized")
    void kakaoLoginMapsInvalidTokenToUnauthorized() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN))
            .when(kakaoLoginService)
            .login(new AuthCommand.KakaoLogin("bad-token", "nonce", null));

        mockMvc.perform(
            post("/api/v1/auth/kakao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"bad-token\",\"nonce\":\"nonce\"}")
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_009")));
    }

    @Test
    @DisplayName("complete onboarding returns onboarding complete response")
    void completeOnboardingReturnsOnboardingCompleteResponse() throws Exception {
        when(
            onboardingService.complete(
                new AuthCommand.CompleteOnboarding(
                    memberId,
                    "홍길동",
                    "010-1234-5678",
                    LocalDate.parse("1990-01-01"),
                    "길동",
                    3,
                    ManagementType.AGRICULTURAL_INDIVIDUAL,
                    farmCommand(),
                    java.util.List.of(cropId),
                    UUID.fromString("00000000-0000-0000-0000-000000000501")
                )
            )
        ).thenReturn(onboardingCompleteResult());

        var result = mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .with(authenticatedMember(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.member.id", equalTo(memberId.toString())))
            .andExpect(jsonPath("$.data.member.nickname", equalTo("길동")))
            .andExpect(jsonPath("$.data.member.experienceLevel", equalTo(3)))
            .andExpect(jsonPath("$.data.member.managementType", equalTo("AGRICULTURAL_INDIVIDUAL")))
            .andExpect(jsonPath("$.data.member.profileImageUrl", equalTo("https://example.test/profile.jpg")))
            .andExpect(jsonPath("$.data.farm.name", equalTo("길동농장")))
            .andExpect(jsonPath("$.data.farm.roadAddress", equalTo("서울시 강남구 테헤란로 1")))
            .andExpect(jsonPath("$.data.farm.jibunAddress", equalTo("서울시 강남구 역삼동 1")))
            .andExpect(jsonPath("$.data.farm.pnu", equalTo("4511310200101230004")))
            .andExpect(jsonPath("$.data.farm.boundaryCoordinates[0].latitude", equalTo(35.8461)))
            .andExpect(jsonPath("$.data.farm.dataSource.address", equalTo("JUSO")))
            .andExpect(jsonPath("$.data.crops[0].id", equalTo(cropId.toString())))
            .andExpect(jsonPath("$.data.crops[0].externalNo", equalTo(1001)))
            .andExpect(jsonPath("$.data.crops[0].name", equalTo("토마토")))
            .andExpect(jsonPath("$.data.crops[0].usePartCategory", equalTo("FRUIT")))
            .andExpect(jsonPath("$.data.crops[0].usePartCategoryLabel", equalTo("열매")))
            .andExpect(jsonPath("$.data.onboarding.status", equalTo("COMPLETE")))
            .andExpect(jsonPath("$.data.onboarding.missingFields").isEmpty())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("reg" + "ion");
    }

    @Test
    @DisplayName("complete onboarding uses name when nickname is omitted")
    void completeOnboardingUsesNameWhenNicknameIsOmitted() throws Exception {
        when(
            onboardingService.complete(
                new AuthCommand.CompleteOnboarding(
                    memberId,
                    "홍길동",
                    "010-1234-5678",
                    LocalDate.parse("1990-01-01"),
                    null,
                    3,
                    ManagementType.AGRICULTURAL_INDIVIDUAL,
                    farmCommand(),
                    java.util.List.of(cropId),
                    UUID.fromString("00000000-0000-0000-0000-000000000501")
                )
            )
        ).thenReturn(
            new AuthResult.OnboardingComplete(completedMemberProfile("홍길동"), onboardingCompleteResult().getFarm(), onboardingCompleteResult().getCrops(), onboardingCompleteResult().getOnboarding())
        );

        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .with(authenticatedMember(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBodyWithoutNickname())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.member.nickname", equalTo("홍길동")));
    }

    @Test
    @DisplayName("complete onboarding rejects malformed principal")
    void completeOnboardingRejectsMalformedPrincipal() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/onboarding/complete")
                .with(authenticatedMember("not-a-uuid"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingRequestBody())
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }

    private AuthResult.Login loginResult() {
        return new AuthResult.Login(
            "access-token",
            "refresh-token",
            memberProfile(),
            new AuthResult.Onboarding(
                AuthResult.OnboardingStatus.REQUIRED,
                java.util.List.of(
                    AuthResult.OnboardingField.NAME,
                    AuthResult.OnboardingField.PHONE,
                    AuthResult.OnboardingField.BIRTH_DATE,
                    AuthResult.OnboardingField.NICKNAME,
                    AuthResult.OnboardingField.EXPERIENCE_LEVEL,
                    AuthResult.OnboardingField.MANAGEMENT_TYPE
                )
            )
        );
    }

    private AuthResult.MemberProfile memberProfile() {
        return new AuthResult.MemberProfile(
            memberId,
            "social@example.com",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private AuthResult.OnboardingComplete onboardingCompleteResult() {
        return new AuthResult.OnboardingComplete(
            completedMemberProfile("길동"),
            new AuthResult.FarmSummary(
                farmId,
                "길동농장",
                "서울시 강남구 테헤란로 1",
                "서울시 강남구 역삼동 1",
                35.8465,
                127.1292,
                "4511310200101230004",
                "전",
                new BigDecimal("1200.5"),
                false,
                java.util.List.of(
                    new AuthResult.FarmBoundaryCoordinateSummary(35.8461, 127.1289),
                    new AuthResult.FarmBoundaryCoordinateSummary(35.8463, 127.1295)
                ),
                new AuthResult.FarmDataSourceSummary(
                    "JUSO",
                    "V_WORLD_ADDRESS",
                    "V_WORLD_CADASTRAL",
                    "V_WORLD_LAND_CHARACTERISTIC"
                )
            ),
            java.util.List.of(
                new CropResult.CropSummary(
                    cropId,
                    1001,
                    "토마토",
                    "FRUIT",
                    "열매"
                )
            ),
            new AuthResult.Onboarding(
                AuthResult.OnboardingStatus.COMPLETE,
                java.util.List.of()
            )
        );
    }

    private AuthResult.MemberProfile completedMemberProfile(String nickname) {
        return new AuthResult.MemberProfile(
            memberId,
            "member@example.com",
            "홍길동",
            "010-1234-5678",
            LocalDate.parse("1990-01-01"),
            nickname,
            3,
            "AGRICULTURAL_INDIVIDUAL",
            "https://example.test/profile.jpg"
        );
    }

    private String validOnboardingRequestBody() {
        return "{\n  \"name\":\"홍길동\",\n  \"phone\":\"010-1234-5678\",\n  \"birthDate\":\"1990-01-01\",\n  \"nickname\":\"길동\",\n  \"experienceLevel\":3,\n  \"managementType\":\"AGRICULTURAL_INDIVIDUAL\",\n  \"profileMediaId\":\"00000000-0000-0000-0000-000000000501\",\n  \"farm\": {\n    \"name\":\"길동농장\",\n    \"roadAddress\":\"서울시 강남구 테헤란로 1\",\n    \"jibunAddress\":\"서울시 강남구 역삼동 1\",\n    \"latitude\":35.8465,\n    \"longitude\":127.1292,\n    \"pnu\":\"4511310200101230004\",\n    \"landCategory\":\"전\",\n    \"areaSqm\":1200.5,\n    \"areaIsManualEntry\":false,\n    \"boundaryCoordinates\":[\n      {\"latitude\":35.8461,\"longitude\":127.1289},\n      {\"latitude\":35.8463,\"longitude\":127.1295}\n    ],\n    \"dataSource\":{\n      \"address\":\"JUSO\",\n      \"coordinate\":\"V_WORLD_ADDRESS\",\n      \"parcel\":\"V_WORLD_CADASTRAL\",\n      \"landCharacteristic\":\"V_WORLD_LAND_CHARACTERISTIC\"\n    }\n  },\n  \"cropIds\":[\"" + (cropId) + "\"]\n}";
    }

    private String validOnboardingRequestBodyWithoutNickname() {
        return validOnboardingRequestBody().replace("\"nickname\":\"길동\",", "");
    }
    private FarmCommand.Draft farmCommand() {
        return new FarmCommand.Draft(
            "길동농장",
            "서울시 강남구 테헤란로 1",
            "서울시 강남구 역삼동 1",
            35.8465,
            127.1292,
            "4511310200101230004",
            "전",
            new BigDecimal("1200.5"),
            false,
            java.util.List.of(
                new FarmCommand.BoundaryCoordinate(35.8461, 127.1289),
                new FarmCommand.BoundaryCoordinate(35.8463, 127.1295)
            ),
            new FarmCommand.DataSource(
                "JUSO",
                "V_WORLD_ADDRESS",
                "V_WORLD_CADASTRAL",
                "V_WORLD_LAND_CHARACTERISTIC"
            )
        );
    }

    private RequestPostProcessor authenticatedMember(String memberId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication( new UsernamePasswordAuthenticationToken(
                memberId,
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
            ));
            return request;
        };
    }

    @Test
    @DisplayName("reissue preserves optional body cookie fallback and body precedence")
    void reissuePreservesCookieFallbackAndBodyPrecedence() throws Exception {
        var cookie = new jakarta.servlet.http.Cookie("refreshToken", "cookie-value");
        when(authService.reissue(new AuthCommand.Reissue("cookie-value")))
            .thenReturn(new AuthResult.TokenPair("rotated-access", "rotated-refresh"));
        when(authService.reissue(new AuthCommand.Reissue("body-value")))
            .thenReturn(new AuthResult.TokenPair("rotated-access", "rotated-refresh"));
        when(authService.reissue(new AuthCommand.Reissue("")))
            .thenReturn(new AuthResult.TokenPair("rotated-access", "rotated-refresh"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        mockMvc.perform(post("/api/v1/auth/reissue").cookie(cookie)).andExpect(status().isOk());
        for (String body : java.util.List.of("{}", "{\"refreshToken\":null}")) {
            mockMvc.perform(post("/api/v1/auth/reissue").cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/auth/reissue").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"body-value\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Path=/")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=120")));
        mockMvc.perform(post("/api/v1/auth/reissue").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"\"}")).andExpect(status().isOk());
        org.mockito.Mockito.verify(authService, org.mockito.Mockito.times(3)).reissue(new AuthCommand.Reissue("cookie-value"));
        org.mockito.Mockito.verify(authService).reissue(new AuthCommand.Reissue("body-value"));
        org.mockito.Mockito.verify(authService).reissue(new AuthCommand.Reissue(""));
        mockMvc.perform(post("/api/v1/auth/reissue")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("logout clears the refresh cookie using the configured security attributes")
    void logoutClearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=;")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Secure")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Path=/")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")));
        org.mockito.Mockito.verify(authService).logout(memberId.toString());
    }

    @Test
    @DisplayName("response mapping preserves list order mutability and reference boundaries")
    void responseMappingPreservesListBoundaries() {
        var result = onboardingCompleteResult();
        assertThat(result.getCrops()).hasSize(1);
        for (int size : java.util.List.of(0, 1, 2)) {
            var crops = new java.util.ArrayList<CropResult.CropSummary>();
            for (int i = 0; i < size; i++) crops.add(result.getCrops().get(0));
            var mapped = com.chamchamcham.api.auth.dto.AuthResponses.OnboardingCompleteResponse.from(
                new AuthResult.OnboardingComplete(result.getMember(), result.getFarm(), crops, result.getOnboarding()));
            assertThat(mapped.getCrops()).hasSize(size);
            mapped.getCrops().add(com.chamchamcham.api.auth.dto.AuthResponses.CropResponse.from(result.getCrops().get(0)));
            assertThat(crops).hasSize(size);
            assertThat(mapped.getCrops()).hasSize(size + 1);
        }
        var missing = new java.util.ArrayList<AuthResult.OnboardingField>();
        missing.add(AuthResult.OnboardingField.NAME);
        var mapped = com.chamchamcham.api.auth.dto.AuthResponses.OnboardingResponse.from(
            new AuthResult.Onboarding(AuthResult.OnboardingStatus.REQUIRED, missing));
        assertThat(mapped.getMissingFields()).isSameAs(missing).containsExactly(AuthResult.OnboardingField.NAME);
    }


    @Test
    @DisplayName("direct onboarding null arguments preserve requireNotNull failure semantics")
    void directOnboardingNullArgumentsPreserveFailureSemantics() {
        var controller = new AuthController(authService, kakaoLoginService, appleLoginService,
            naverLoginService, onboardingService, tokenProvider, true);
        for (var request : java.util.List.of(
            new com.chamchamcham.api.auth.dto.AuthRequests.CompleteOnboardingRequest(null, null, null, null,
                null, null, null, java.util.List.of(), null),
            new com.chamchamcham.api.auth.dto.AuthRequests.CompleteOnboardingRequest(null, null, null, null,
                3, null, null, java.util.List.of(), null),
            new com.chamchamcham.api.auth.dto.AuthRequests.CompleteOnboardingRequest(null, null, null, null,
                3, ManagementType.AGRICULTURAL_INDIVIDUAL, null, java.util.List.of(), null))) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.completeOnboarding(memberId.toString(), request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Required value was null.");
        }
        org.mockito.Mockito.verifyNoInteractions(onboardingService);
    }
}
