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

class MidTermLandRegionTest {
    private final Set<String> validLandRegIds = Set.of("11B00000", "11C10000", "11C20000", "11D10000", "11D20000", "11F10000", "11F20000", "11G00000", "11H10000", "11H20000");
    @Test
    @DisplayName("모든 중기예보 지점이 유효한 육상 구역코드로 매핑된다")
    void test01() {
        var unmapped = MidFcstStations.INSTANCE.getALL().stream().filter(it -> MidTermLandRegion.INSTANCE.of(it.getTaRegId()) == null).toList();
        assertThat(unmapped)
            .describedAs("매핑되지 않는 지점이 있으면 그 지역은 주간예보 5일째 날씨 상태를 얻지 못한다")
            .isEmpty();
    }

    @Test
    @DisplayName("매핑 결과는 기상청이 받아주는 10개 코드 중 하나다")
    void test02() {
        var produced = MidFcstStations.INSTANCE.getALL().stream().map(it -> MidTermLandRegion.INSTANCE.of(it.getTaRegId())).filter(Objects::nonNull).collect(Collectors.toSet());
        assertThat(produced).isSubsetOf(validLandRegIds);
    }

    @ParameterizedTest
    @CsvSource({
        "11B10101, 11B00000",
        "11B20201, 11B00000",
        "11A00101, 11B00000",
        "11E00101, 11H10000",
        "11E00102, 11H10000",
        "11G00800, 11G00000",
        "11C10301, 11C10000",
        "11C20401, 11C20000",
        "11D10301, 11D10000",
        "11D20501, 11D20000",
        "11F10201, 11F10000",
        "11F20501, 11F20000",
        "11H10701, 11H10000",
        "11H20201, 11H20000"
    })
    @DisplayName("taRegId 접두 4자리로 육상 구역코드를 정한다")
    void test03(String taRegId, String expected) {
        assertThat(MidTermLandRegion.INSTANCE.of(taRegId)).isEqualTo(expected);
    }

    @Test
    @DisplayName("알 수 없는 접두사는 null이다")
    void test04() {
        assertThat(MidTermLandRegion.INSTANCE.of("99Z00000")).isNull();
        assertThat(MidTermLandRegion.INSTANCE.of("")).isNull();
    }

}
