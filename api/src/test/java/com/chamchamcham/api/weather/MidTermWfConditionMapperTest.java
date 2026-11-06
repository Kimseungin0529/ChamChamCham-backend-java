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

class MidTermWfConditionMapperTest {

    @ParameterizedTest
    @CsvSource({
        "맑음, CLEAR",
        "구름많음, PARTLY_CLOUDY",
        "흐림, CLOUDY",
        "구름많고 비, RAIN",
        "구름많고 눈, SNOW",
        "구름많고 비/눈, RAIN_SNOW",
        "구름많고 소나기, SHOWER",
        "흐리고 비, RAIN",
        "흐리고 눈, SNOW",
        "흐리고 비/눈, RAIN_SNOW",
        "흐리고 소나기, SHOWER"
    })
    @DisplayName("기상청 어휘 11개를 판정한다")
    void test01(String wf, WeatherCondition expected) {
        var result = MidTermWfConditionMapper.INSTANCE.of(wf);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("흐림이 흐리 부분매칭 실패 없이 흐림으로 판정된다")
    void test02() {
        var result = MidTermWfConditionMapper.INSTANCE.of("흐림");
        assertThat(result).isEqualTo(WeatherCondition.CLOUDY);
    }

    @Test
    @DisplayName("흐리고 비 눈은 강수 표현이 우선해 비 눈으로 판정된다")
    void test03() {
        var result = MidTermWfConditionMapper.INSTANCE.of("흐리고 비/눈");
        assertThat(result).isEqualTo(WeatherCondition.RAIN_SNOW);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null이거나 빈 문자열이면 정보없음을 반환한다")
    void test04(String wf) {
        var result = MidTermWfConditionMapper.INSTANCE.of(wf);
        assertThat(result).isEqualTo(WeatherCondition.UNKNOWN);
    }

}
