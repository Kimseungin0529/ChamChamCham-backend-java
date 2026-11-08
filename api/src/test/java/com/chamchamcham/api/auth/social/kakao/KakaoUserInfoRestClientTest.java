package com.chamchamcham.api.auth.social.kakao;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;


class KakaoUserInfoRestClientTest {
    private MockRestServiceServer server;
    private KakaoUserInfoRestClient client;

    @BeforeEach
    void setUp() throws Exception {
        var restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new KakaoUserInfoRestClient(restClientBuilder.build(), USER_INFO_URI);
    }

    @Test
    @DisplayName("fetch maps successful userinfo and sends bearer header")
    void fetchMapsSuccessfulUserinfoAndSendsBearerHeader() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(USER_INFO_URI))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        "{\n  \"sub\": \"kakao-sub\",\n  \"email\": \"kakao@example.com\",\n  \"email_verified\": true,\n  \"name\": \"카카오회원\",\n  \"phone_number\": \"010-1234-5678\",\n  \"phone_number_verified\": true,\n  \"birthdate\": \"1990-05-12\"\n}"
                    )
            );

        var userInfo = client.fetch("access-token");

        assertEquals("kakao-sub", userInfo.getSubject());
        assertEquals("kakao@example.com", userInfo.getEmail());
        assertEquals("카카오회원", userInfo.getName());
        assertEquals("010-1234-5678", userInfo.getPhone());
        assertEquals(LocalDate.of(1990, 5, 12), userInfo.getBirthDate());
        server.verify();
    }

    @Test
    @DisplayName("fetch maps unauthorized response to invalid kakao token")
    void fetchMapsUnauthorizedResponseToInvalidKakaoToken() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps forbidden response to invalid kakao token")
    void fetchMapsForbiddenResponseToInvalidKakaoToken() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps generic client error response to invalid kakao token")
    void fetchMapsGenericClientErrorResponseToInvalidKakaoToken() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps provider server failure to unavailable")
    void fetchMapsProviderServerFailureToUnavailable() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.KAKAO_OIDC_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps transport exception to unavailable")
    void fetchMapsTransportExceptionToUnavailable() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(withException(new IOException("connection reset")));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.KAKAO_OIDC_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps null response body to unavailable")
    void fetchMapsNullResponseBodyToUnavailable() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(withStatus(HttpStatus.OK));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.KAKAO_OIDC_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps missing subject to invalid kakao token")
    void fetchMapsMissingSubjectToInvalidKakaoToken() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"email\":\"kakao@example.com\",\"email_verified\":true}")
            );

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps blank subject to invalid kakao token")
    void fetchMapsBlankSubjectToInvalidKakaoToken() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sub\":\" \"}")
            );

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps unverified email to null")
    void fetchMapsUnverifiedEmailToNull() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sub\":\"kakao-sub\",\"email\":\"kakao@example.com\",\"email_verified\":false}")
            );

        var userInfo = client.fetch("access-token");

        assertNull(userInfo.getEmail());
    }

    @Test
    @DisplayName("fetch maps unverified phone to null")
    void fetchMapsUnverifiedPhoneToNull() throws Exception {
        server.expect(requestTo(USER_INFO_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sub\":\"kakao-sub\",\"phone_number\":\"010-1234-5678\",\"phone_number_verified\":false}")
            );

        var userInfo = client.fetch("access-token");

        assertNull(userInfo.getPhone());
    }

    @Test
    @DisplayName("fetch maps partial and placeholder birthdate to null")
    void fetchMapsPartialAndPlaceholderBirthdateToNull() throws Exception {
        var birthdates = java.util.List.of("1990", "0000-05-12", "1990-13-40");
        for (String birthdate : birthdates) {
            server.expect(requestTo(USER_INFO_URI))
                .andRespond(
                    withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"sub\":\"kakao-sub\",\"birthdate\":\"" + (birthdate) + "\"}")
                );
        }

        for (String ignored : birthdates) {
            var userInfo = client.fetch("access-token");

            assertNull(userInfo.getBirthDate());
        }
    }

    private static final String USER_INFO_URI = "https://kakao.example.test/userinfo";
}
