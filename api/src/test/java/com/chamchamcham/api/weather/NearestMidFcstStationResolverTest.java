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

class NearestMidFcstStationResolverTest {
    private final MidFcstStation seoul = new MidFcstStation("11B10101","서울",37.56609444,126.9774167);
    private final MidFcstStation busan = new MidFcstStation("11H20201","부산",35.104683,129.032013);
    private final MidFcstStation jeju = new MidFcstStation("11G00201","제주",33.486275,126.4979528);
    private final List<MidFcstStation> fixture = Arrays.asList(seoul,busan,jeju);
    @Test
    @DisplayName("서울 인근 좌표는 서울 지점을 반환한다")
    void test01() {
        var result = NearestMidFcstStationResolver.INSTANCE.resolve(37.5665, 126.9780, fixture);
        assertThat(result).isEqualTo(seoul);
    }

    @Test
    @DisplayName("부산 인근 좌표는 부산 지점을 반환한다")
    void test02() {
        var result = NearestMidFcstStationResolver.INSTANCE.resolve(35.1796, 129.0756, fixture);
        assertThat(result).isEqualTo(busan);
    }

    @Test
    @DisplayName("제주 인근 좌표는 제주 지점을 반환한다")
    void test03() {
        var result = NearestMidFcstStationResolver.INSTANCE.resolve(33.4996, 126.5312, fixture);
        assertThat(result).isEqualTo(jeju);
        assertThat(result.getTaRegId()).isEqualTo("11G00201");
    }

    @Test
    @DisplayName("기본 인자는 전체 중기예보 지점 목록을 사용한다")
    void test04() {
        var result = NearestMidFcstStationResolver.INSTANCE.resolve(37.5665, 126.9780);
        assertThat(result.getTaRegId()).isEqualTo("11B10101");
    }

}
