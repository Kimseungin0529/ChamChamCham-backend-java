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

class NearestAsosStationResolverTest {
    private final AsosStation seoul = new AsosStation("108","서울",37.5714,126.9658);
    private final AsosStation busan = new AsosStation("159","부산",35.1047,129.032);
    private final AsosStation jeju = new AsosStation("184","제주",33.5141,126.5297);
    private final List<AsosStation> fixture = Arrays.asList(seoul,busan,jeju);
    @Test
    @DisplayName("서울 인근 좌표는 서울 지점을 반환한다")
    void test01() {
        var result = NearestAsosStationResolver.INSTANCE.resolve(37.5665, 126.9780, fixture);
        assertThat(result).isEqualTo(seoul);
    }

    @Test
    @DisplayName("부산 인근 좌표는 부산 지점을 반환한다")
    void test02() {
        var result = NearestAsosStationResolver.INSTANCE.resolve(35.1796, 129.0756, fixture);
        assertThat(result).isEqualTo(busan);
    }

    @Test
    @DisplayName("제주 인근 좌표는 제주 지점을 반환한다")
    void test03() {
        var result = NearestAsosStationResolver.INSTANCE.resolve(33.4996, 126.5312, fixture);
        assertThat(result).isEqualTo(jeju);
    }

    @Test
    @DisplayName("기본 인자는 전체 ASOS 지점 목록을 사용한다")
    void test04() {
        var result = NearestAsosStationResolver.INSTANCE.resolve(37.5665, 126.9780);
        assertThat(result.getId()).isEqualTo("108");
    }

}
