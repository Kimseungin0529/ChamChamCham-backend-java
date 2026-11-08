package com.chamchamcham.api.auth.social.kakao;

import com.chamchamcham.application.auth.social.KakaoUserInfoClient;
import com.chamchamcham.application.auth.social.KakaoUserInfo;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KakaoUserInfoRestClient implements KakaoUserInfoClient {
    private final RestClient restClient;
    private final String userInfoUri;

    public KakaoUserInfoRestClient(RestClient restClient, String userInfoUri) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.userInfoUri = Objects.requireNonNull(userInfoUri, "userInfoUri");
    }

    @Autowired
    public KakaoUserInfoRestClient(RestClient.Builder restClientBuilder,
        @Value("${auth.kakao.oidc.user-info-uri:https://kapi.kakao.com/v1/oidc/userinfo}") String userInfoUri,
        @Value("${auth.kakao.oidc.connect-timeout-millis:2000}") int connectTimeoutMillis,
        @Value("${auth.kakao.oidc.read-timeout-millis:2000}") int readTimeoutMillis) {
        this(restClientBuilder.requestFactory(createRequestFactory(connectTimeoutMillis, readTimeoutMillis)).build(), userInfoUri);
    }

    @Override
    public KakaoUserInfo fetch(String accessToken) {
        Objects.requireNonNull(accessToken, "accessToken");
        final KakaoUserInfoResponse body;
        try {
            body = restClient.get().uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).retrieve().body(KakaoUserInfoResponse.class);
        } catch (RestClientResponseException exception) {
            throw new BusinessException(exception.getStatusCode().is4xxClientError() ? ErrorCode.INVALID_KAKAO_TOKEN : ErrorCode.KAKAO_OIDC_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.KAKAO_OIDC_UNAVAILABLE);
        }
        if (body == null) throw new BusinessException(ErrorCode.KAKAO_OIDC_UNAVAILABLE);
        var subject = body.subject();
        if (subject == null || blank(subject)) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        return new KakaoUserInfo(subject, Boolean.TRUE.equals(body.emailVerified()) ? body.email() : null, body.name(),
            Boolean.TRUE.equals(body.phoneNumberVerified()) ? body.phoneNumber() : null, parseBirthDate(body.birthdate()));
    }

    @Nullable
    private LocalDate parseBirthDate(@Nullable String birthdate) {
        if (birthdate == null || !birthdate.matches("\\d{4}-\\d{2}-\\d{2}") || birthdate.startsWith("0000-")) return null;
        try {
            return LocalDate.parse(birthdate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record KakaoUserInfoResponse(
        @JsonProperty("sub") @Nullable String subject,
        @Nullable String name,
        @Nullable String email,
        @JsonProperty("email_verified") @Nullable Boolean emailVerified,
        @Nullable String birthdate,
        @JsonProperty("phone_number") @Nullable String phoneNumber,
        @JsonProperty("phone_number_verified") @Nullable Boolean phoneNumberVerified
    ) {}

    private static SimpleClientHttpRequestFactory createRequestFactory(int connectTimeoutMillis, int readTimeoutMillis) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMillis);
        factory.setReadTimeout(readTimeoutMillis);
        return factory;
    }

    private static boolean blank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
}
