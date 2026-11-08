package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.*;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AsosConditionMapperTest {

    @Test
    @DisplayName("일강수량이 0보다 크면 전운량과 무관하게 비를 반환한다")
    void test01() {
        var result = AsosConditionMapper.INSTANCE.of(0.0, 0.1);
        assertThat(result).isEqualTo(WeatherCondition.RAIN);
    }

    @Test
    @DisplayName("일강수량이 0이면 강수로 취급하지 않는다")
    void test02() {
        var result = AsosConditionMapper.INSTANCE.of(1.0, 0.0);
        assertThat(result).isEqualTo(WeatherCondition.CLEAR);
    }

    @Test
    @DisplayName("일강수량이 null이면 강수로 취급하지 않는다")
    void test03() {
        var result = AsosConditionMapper.INSTANCE.of(1.0, null);
        assertThat(result).isEqualTo(WeatherCondition.CLEAR);
    }

    @Test
    @DisplayName("평균 전운량이 null이면 정보없음을 반환한다")
    void test04() {
        var result = AsosConditionMapper.INSTANCE.of(null, null);
        assertThat(result).isEqualTo(WeatherCondition.UNKNOWN);
    }

    @Test
    @DisplayName("평균 전운량이 2 point 0이면 맑음을 반환한다")
    void test05() {
        var result = AsosConditionMapper.INSTANCE.of(2.0, null);
        assertThat(result).isEqualTo(WeatherCondition.CLEAR);
    }

    @Test
    @DisplayName("평균 전운량이 2 point 1이면 구름많음을 반환한다")
    void test06() {
        var result = AsosConditionMapper.INSTANCE.of(2.1, null);
        assertThat(result).isEqualTo(WeatherCondition.PARTLY_CLOUDY);
    }

    @Test
    @DisplayName("평균 전운량이 7 point 0이면 구름많음을 반환한다")
    void test07() {
        var result = AsosConditionMapper.INSTANCE.of(7.0, null);
        assertThat(result).isEqualTo(WeatherCondition.PARTLY_CLOUDY);
    }

    @Test
    @DisplayName("평균 전운량이 7 point 1이면 흐림을 반환한다")
    void test08() {
        var result = AsosConditionMapper.INSTANCE.of(7.1, null);
        assertThat(result).isEqualTo(WeatherCondition.CLOUDY);
    }

}
