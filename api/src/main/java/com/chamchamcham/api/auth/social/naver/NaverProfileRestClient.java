package com.chamchamcham.api.auth.social.naver;

import com.chamchamcham.application.auth.social.NaverProfileClient;
import com.chamchamcham.application.auth.social.NaverProfile;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
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
public class NaverProfileRestClient implements NaverProfileClient {
    private final RestClient restClient;
    private final String profileUri;

    public NaverProfileRestClient(RestClient restClient, String profileUri) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.profileUri = Objects.requireNonNull(profileUri, "profileUri");
    }

    @Autowired
    public NaverProfileRestClient(RestClient.Builder restClientBuilder,
        @Value("${auth.naver.profile-uri:https://openapi.naver.com/v1/nid/me}") String profileUri,
        @Value("${auth.naver.connect-timeout-millis:2000}") int connectTimeoutMillis,
        @Value("${auth.naver.read-timeout-millis:2000}") int readTimeoutMillis) {
        this(restClientBuilder.requestFactory(createRequestFactory(connectTimeoutMillis, readTimeoutMillis)).build(), profileUri);
    }

    @Override
    public NaverProfile fetch(String accessToken) {
        Objects.requireNonNull(accessToken, "accessToken");
        final NaverProfileResponse body;
        try {
            body = restClient.get().uri(profileUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).retrieve().body(NaverProfileResponse.class);
        } catch (RestClientResponseException exception) {
            throw new BusinessException(exception.getStatusCode().is4xxClientError() ? ErrorCode.INVALID_NAVER_TOKEN : ErrorCode.NAVER_PROFILE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.NAVER_PROFILE_UNAVAILABLE);
        }
        if (body == null) throw new BusinessException(ErrorCode.NAVER_PROFILE_UNAVAILABLE);
        if (!"00".equals(body.resultcode())) throw new BusinessException(ErrorCode.INVALID_NAVER_TOKEN);
        var profile = body.response();
        if (profile == null) throw new BusinessException(ErrorCode.NAVER_PROFILE_UNAVAILABLE);
        var subject = profile.id();
        if (subject == null || blank(subject)) throw new BusinessException(ErrorCode.INVALID_NAVER_TOKEN);
        return new NaverProfile(subject, profile.email(), profile.name(), profile.mobile(),
            parseBirthDate(profile.birthyear(), profile.birthday()));
    }

    @Nullable
    private LocalDate parseBirthDate(@Nullable String birthyear, @Nullable String birthday) {
        if (birthyear == null || blank(birthyear) || birthday == null || blank(birthday)) return null;
        try {
            return LocalDate.parse(birthyear + "-" + birthday);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record NaverProfileResponse(@Nullable String resultcode, @Nullable String message, @Nullable NaverProfilePayload response) {}
    private record NaverProfilePayload(@Nullable String id, @Nullable String email, @Nullable String name,
        @Nullable String mobile, @Nullable String birthyear, @Nullable String birthday) {}

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
