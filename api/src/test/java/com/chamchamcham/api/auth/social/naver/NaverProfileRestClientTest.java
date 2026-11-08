package com.chamchamcham.api.auth.social.naver;

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


class NaverProfileRestClientTest {
    private MockRestServiceServer server;
    private NaverProfileRestClient client;

    @BeforeEach
    void setUp() throws Exception {
        var restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new NaverProfileRestClient(restClientBuilder.build(), PROFILE_URI);
    }

    @Test
    @DisplayName("fetch maps successful profile and sends bearer header")
    void fetchMapsSuccessfulProfileAndSendsBearerHeader() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(PROFILE_URI))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        "{\n  \"resultcode\": \"00\",\n  \"message\": \"success\",\n  \"response\": {\n    \"id\": \"naver-sub\",\n    \"email\": \"naver@example.com\",\n    \"name\": \"네이버회원\",\n    \"mobile\": \"010-1234-5678\",\n    \"birthyear\": \"1990\",\n    \"birthday\": \"05-12\"\n  }\n}"
                    )
            );

        var profile = client.fetch("access-token");

        assertEquals("naver-sub", profile.getSubject());
        assertEquals("naver@example.com", profile.getEmail());
        assertEquals("네이버회원", profile.getName());
        assertEquals("010-1234-5678", profile.getPhone());
        assertEquals(LocalDate.of(1990, 5, 12), profile.getBirthDate());
        server.verify();
    }

    @Test
    @DisplayName("fetch maps provider server failure to unavailable")
    void fetchMapsProviderServerFailureToUnavailable() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.NAVER_PROFILE_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps unauthorized response to invalid naver token")
    void fetchMapsUnauthorizedResponseToInvalidNaverToken() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_NAVER_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps forbidden response to invalid naver token")
    void fetchMapsForbiddenResponseToInvalidNaverToken() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_NAVER_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps generic client error response to invalid naver token")
    void fetchMapsGenericClientErrorResponseToInvalidNaverToken() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_NAVER_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps transport exception to unavailable")
    void fetchMapsTransportExceptionToUnavailable() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(withException(new IOException("connection reset")));

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.NAVER_PROFILE_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps provider failure resultcode to invalid token")
    void fetchMapsProviderFailureResultcodeToInvalidToken() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"resultcode\":\"024\",\"message\":\"Authentication failed\"}")
            );

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_NAVER_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps missing profile body to unavailable")
    void fetchMapsMissingProfileBodyToUnavailable() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"resultcode\":\"00\",\"message\":\"success\"}")
            );

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.NAVER_PROFILE_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps blank profile id to invalid token")
    void fetchMapsBlankProfileIdToInvalidToken() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"resultcode\":\"00\",\"message\":\"success\",\"response\":{\"id\":\" \"}}")
            );

        var exception = assertThrows(BusinessException.class, () -> {
            client.fetch("access-token");
        });

        assertEquals(ErrorCode.INVALID_NAVER_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("fetch maps invalid birth data to null")
    void fetchMapsInvalidBirthDataToNull() throws Exception {
        server.expect(requestTo(PROFILE_URI))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        "{\n  \"resultcode\": \"00\",\n  \"message\": \"success\",\n  \"response\": {\n    \"id\": \"naver-sub\",\n    \"email\": \"naver@example.com\",\n    \"birthyear\": \"1990\",\n    \"birthday\": \"13-40\"\n  }\n}"
                    )
            );

        var profile = client.fetch("access-token");

        assertNull(profile.getBirthDate());
    }

    private static final String PROFILE_URI = "https://naver.example.test/profile";
}
