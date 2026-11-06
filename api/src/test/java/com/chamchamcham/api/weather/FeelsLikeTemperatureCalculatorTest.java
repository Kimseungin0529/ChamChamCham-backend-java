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

class FeelsLikeTemperatureCalculatorTest {

    @Test
    @DisplayName("여름철에는 열지수 공식으로 체감온도를 계산한다")
    void test01() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            30,
            50,
            null,
            7
        );
        assertThat(result).isEqualTo(30);
    }

    @Test
    @DisplayName("겨울철 적용조건을 만족하면 풍속냉각 공식으로 체감온도를 계산한다")
    void test02() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            0,
            null,
            10.0,
            1
        );
        assertThat(result).isEqualTo(-7);
    }

    @Test
    @DisplayName("겨울철 기온이 10도를 초과하면 공식을 적용하지 않고 원본 기온을 반환한다")
    void test03() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            15,
            null,
            10.0,
            12
        );
        assertThat(result).isEqualTo(15);
    }

    @Test
    @DisplayName("겨울철 풍속이 1_3 미만이면 공식을 적용하지 않고 원본 기온을 반환한다")
    void test04() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            0,
            null,
            1.0,
            2
        );
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("여름철에 습도가 없으면 null을 반환한다")
    void test05() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            30,
            null,
            null,
            8
        );
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("겨울철에 풍속이 없으면 null을 반환한다")
    void test06() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            0,
            null,
            null,
            10
        );
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("겨울철 기온이 정확히 10도이면 조건을 만족해 공식을 적용한다")
    void test07() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            10,
            null,
            10.0,
            11
        );
        assertThat(result).isEqualTo(6);
    }

    @Test
    @DisplayName("겨울철 풍속이 정확히 1_3이면 조건을 만족해 공식을 적용한다")
    void test08() {
        var result = FeelsLikeTemperatureCalculator.INSTANCE.of(
            0,
            null,
            1.3,
            3
        );
        assertThat(result).isEqualTo(-1);
    }

}
