package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class KmaBaseTimeTest {
    private final ZoneId zone = ZoneId.of("Asia/Seoul");
    @Test
    @DisplayName("00시 05분 - 어제 2300 최신 어제2300 중기 어제1800")
    void test01() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 0, 5));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260714", "2300"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260714", "2300"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607141800");
    }

    @Test
    @DisplayName("02시 09분 - todayRange latest 모두 어제 2300, 중기는 어제1800")
    void test02() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 2, 9));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260714", "2300"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260714", "2300"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607141800");
    }

    @Test
    @DisplayName("02시 10분 - todayRange latest 모두 오늘 0200, 중기는 여전히 어제1800")
    void test03() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 2, 10));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607141800");
    }

    @Test
    @DisplayName("06시 19분 - todayRange는 여전히 0200, latest는 0500, 중기는 아직 어제1800")
    void test04() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 6, 19));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "0500"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607141800");
    }

    @Test
    @DisplayName("06시 20분 - 중기가 오늘0600으로 넘어간다")
    void test05() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 6, 20));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "0500"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607150600");
    }

    @Test
    @DisplayName("10시 - todayRange는 여전히 0200이다(회귀 방지 핵심)")
    void test06() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "0800"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607150600");
    }

    @Test
    @DisplayName("14시 10분 - todayRange는 여전히 0200이다(옛 버그가 17시 이후로 오진했던 경계)")
    void test07() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 14, 10));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "1400"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607150600");
    }

    @Test
    @DisplayName("18시 19분 - 중기는 아직 오늘0600")
    void test08() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 18, 19));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "1700"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607150600");
    }

    @Test
    @DisplayName("18시 20분 - 중기가 오늘1800으로 넘어간다")
    void test09() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 18, 20));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "1700"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607151800");
    }

    @Test
    @DisplayName("21시 50분 - todayRange는 여전히 0200이다(회귀 방지 핵심)")
    void test10() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 21, 50));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "2000"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607151800");
    }

    @Test
    @DisplayName("23시 10분 - todayRange는 여전히 0200, latest는 2300")
    void test11() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 23, 10));
        assertThat(kmaBaseTime.resolveTodayRange()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
        assertThat(kmaBaseTime.resolveLatest()).isEqualTo(new KmaBaseDateTime("20260715", "2300"));
        assertThat(kmaBaseTime.resolveMidFcst()).isEqualTo("202607151800");
    }

    @Test
    @DisplayName("초단기실황은 39분이면 직전 정시를 사용한다")
    void test12() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 10, 39));
        assertThat(kmaBaseTime.resolveNcst()).isEqualTo(new KmaBaseDateTime("20260715", "0900"));
    }

    @Test
    @DisplayName("초단기실황은 40분이면 현재 정시를 사용한다")
    void test13() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 10, 40));
        assertThat(kmaBaseTime.resolveNcst()).isEqualTo(new KmaBaseDateTime("20260715", "1000"));
    }

    @Test
    @DisplayName("초단기실황은 00시 30분이면 전날 23시로 넘어간다")
    void test14() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 0, 30));
        assertThat(kmaBaseTime.resolveNcst()).isEqualTo(new KmaBaseDateTime("20260714", "2300"));
    }

    @Test
    @DisplayName("자외선지수는 00시 00분이면 00시로 내림한다")
    void test15() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 0, 0));
        assertThat(kmaBaseTime.resolveUv()).isEqualTo("2026071500");
    }

    @Test
    @DisplayName("자외선지수는 02시 59분이면 00시로 내림한다")
    void test16() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 2, 59));
        assertThat(kmaBaseTime.resolveUv()).isEqualTo("2026071500");
    }

    @Test
    @DisplayName("자외선지수는 03시 00분이면 그대로 03시를 사용한다")
    void test17() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 3, 0));
        assertThat(kmaBaseTime.resolveUv()).isEqualTo("2026071503");
    }

    @Test
    @DisplayName("자외선지수는 23시 59분이면 21시로 내림한다")
    void test18() {
        var kmaBaseTime = kmaBaseTimeAt(LocalDateTime.of(2026, 7, 15, 23, 59));
        assertThat(kmaBaseTime.resolveUv()).isEqualTo("2026071521");
    }

    @Test
    @DisplayName("KST 고정 Clock을 fixed Instant로 만들어도 시각 계산이 KST 기준으로 된다")
    void test19() {
        var utcInstant = LocalDateTime.of(2026, 7, 14, 18, 5).toInstant(ZoneOffset.UTC);
        var kmaBaseTime = new KmaBaseTime(Clock.fixed(utcInstant, zone));
        assertThat(kmaBaseTime.resolveNcst()).isEqualTo(new KmaBaseDateTime("20260715", "0200"));
    }
    private KmaBaseTime kmaBaseTimeAt(LocalDateTime time) {
        return new KmaBaseTime(Clock.fixed(time.atZone(zone).toInstant(), zone));
    }
}
