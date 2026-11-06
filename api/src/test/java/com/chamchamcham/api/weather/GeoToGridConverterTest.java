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

class GeoToGridConverterTest {

    @Test
    @DisplayName("서울시청 위경도를 기상청 격자 좌표로 변환한다")
    void test01() {
        var grid = GeoToGridConverter.INSTANCE.convert(37.5665, 126.9780);
        assertThat(grid).isEqualTo(new KmaGrid(60, 127));
    }

    @Test
    @DisplayName("동일 입력은 항상 동일한 격자를 반환한다")
    void test02() {
        var first = GeoToGridConverter.INSTANCE.convert(35.1796, 129.0756);
        var second = GeoToGridConverter.INSTANCE.convert(35.1796, 129.0756);
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("서로 다른 좌표는 서로 다른 격자를 반환한다")
    void test03() {
        var seoul = GeoToGridConverter.INSTANCE.convert(37.5665, 126.9780);
        var busan = GeoToGridConverter.INSTANCE.convert(35.1796, 129.0756);
        assertThat(seoul).isNotEqualTo(busan);
    }

}
