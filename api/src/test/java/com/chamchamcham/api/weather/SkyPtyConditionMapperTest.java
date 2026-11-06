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

class SkyPtyConditionMapperTest {

    @ParameterizedTest
    @CsvSource({
        "1, RAIN",
        "2, RAIN_SNOW",
        "3, SNOW",
        "4, SHOWER",
        "5, DRIZZLE",
        "6, DRIZZLE_SNOW",
        "7, SNOW_FLURRY"
    })
    @DisplayName("PTY가 1부터 7까지면 PTY로 판정한다")
    void test01(String pty, WeatherCondition expected) {
        var result = SkyPtyConditionMapper.INSTANCE.of("1", pty);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "1, CLEAR",
        "3, PARTLY_CLOUDY",
        "4, CLOUDY"
    })
    @DisplayName("PTY가 0이면 SKY로 판정한다")
    void test02(String sky, WeatherCondition expected) {
        var result = SkyPtyConditionMapper.INSTANCE.of(sky, "0");
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("PTY가 null이거나 빈 문자열이면 SKY로 판정한다")
    void test03(String pty) {
        var result = SkyPtyConditionMapper.INSTANCE.of("1", pty);
        assertThat(result).isEqualTo(WeatherCondition.CLEAR);
    }

    @Test
    @DisplayName("PTY가 강수 중이면 SKY보다 우선한다")
    void test04() {
        var result = SkyPtyConditionMapper.INSTANCE.of("1", "1");
        assertThat(result).isEqualTo(WeatherCondition.RAIN);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("SKY가 null이거나 빈 문자열이고 PTY도 없으면 정보없음을 반환한다")
    void test05(String sky) {
        var result = SkyPtyConditionMapper.INSTANCE.of(sky, null);
        assertThat(result).isEqualTo(WeatherCondition.UNKNOWN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "9", "abc"})
    @DisplayName("알 수 없는 SKY 코드면 정보없음을 반환한다")
    void test06(String sky) {
        var result = SkyPtyConditionMapper.INSTANCE.of(sky, null);
        assertThat(result).isEqualTo(WeatherCondition.UNKNOWN);
    }

    @Test
    @DisplayName("알 수 없는 PTY 코드면 SKY로 판정한다")
    void test07() {
        var result = SkyPtyConditionMapper.INSTANCE.of("1", "abc");
        assertThat(result).isEqualTo(WeatherCondition.CLEAR);
    }

}
