package com.chamchamcham.application.weather;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WeatherParallelFetcherTest {
    @Mock private CurrentObservationPort currentObservationPort;
    @Mock private ShortTermForecastPort shortTermForecastPort;
    @Mock private MidTermForecastPort midTermForecastPort;
    @Mock private UvIndexPort uvIndexPort;
    private WeatherParallelFetcher fetcher;
    private Clock clock;
    private WeatherLocation location;
    private LocalDate today;
    private LocalDate d4Date;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(ZonedDateTime.of(2026, 7, 15, 21, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        today = LocalDate.of(2026, 7, 15);
        d4Date = LocalDate.of(2026, 7, 19);
        location = new WeatherLocation(37.5, 127.0, "서울특별시 중구 세종대로 110", "1114010100");
        fetcher = new WeatherParallelFetcher(currentObservationPort, shortTermForecastPort, midTermForecastPort, uvIndexPort, clock);
    }

    @AfterEach
    void tearDown() { fetcher.destroy(); }

    @Test
    void 필수_소스인_실황이_실패하면_요청_전체를_폴백으로_채우고_missing에_current가_담긴다() {
        given(currentObservationPort.fetch(location)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        given(shortTermForecastPort.fetchLatest(location)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        given(shortTermForecastPort.fetchTodayRange(location)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        given(midTermForecastPort.fetch(location, d4Date)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        given(uvIndexPort.fetch(location)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        DetailSources result = fetcher.fetchDetail(location);
        assertEquals(20, result.getCurrent().getTemperature());
        assertNull(result.getCurrent().getPrecipitationType());
        assertEquals(LocalDateTime.of(2026, 7, 15, 21, 0), result.getCurrent().getObservedAt());
        assertEquals(WeatherCondition.CLOUDY, result.getLatest().getCurrentSky());
        assertEquals(20, result.getTodayRange().getMinTemperature());
        assertNotNull(result.getMidTermD4());
        assertEquals(0, result.getUvIndex());
        assertTrue(result.getPartial().getDegraded());
        assertThat(result.getPartial().getMissing()).containsExactly("current", "todayMinMax", "forecast", "forecast.D4", "uvIndex");
    }

    @Test
    void fetchHome도_실황_실패_시_흐림_20도_폴백으로_채운다() {
        given(currentObservationPort.fetch(location)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        given(shortTermForecastPort.fetchLatest(location)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        given(shortTermForecastPort.fetchTodayRange(location)).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        HomeSources result = fetcher.fetchHome(location);
        assertEquals(20, result.getCurrent().getTemperature());
        assertEquals(WeatherCondition.CLOUDY, result.getLatest().getCurrentSky());
        assertEquals(20, result.getTodayRange().getMinTemperature());
        assertTrue(result.getPartial().getDegraded());
        assertThat(result.getPartial().getMissing()).containsExactly("current", "todayMinMax", "forecast");
    }

    @Test
    void 자외선이_예외를_던져도_나머지는_정상이고_missing에_uvIndex가_담긴다() {
        given(currentObservationPort.fetch(location)).willReturn(currentObservation());
        given(shortTermForecastPort.fetchLatest(location)).willReturn(shortTermForecast(true));
        given(shortTermForecastPort.fetchTodayRange(location)).willReturn(dailyForecast(today));
        given(uvIndexPort.fetch(location)).willThrow(new RuntimeException("boom"));
        DetailSources result = fetcher.fetchDetail(location);
        assertNotNull(result.getCurrent());
        assertNotNull(result.getLatest());
        assertNotNull(result.getTodayRange());
        assertNull(result.getUvIndex());
        assertThat(result.getPartial().getMissing()).containsExactly("uvIndex");
        assertTrue(result.getPartial().getDegraded());
    }

    @Test
    void 중기예보가_예외를_던져도_나머지는_정상이고_missing에_forecast_D4가_담긴다() {
        given(currentObservationPort.fetch(location)).willReturn(currentObservation());
        given(shortTermForecastPort.fetchLatest(location)).willReturn(shortTermForecast(false));
        given(shortTermForecastPort.fetchTodayRange(location)).willReturn(dailyForecast(today));
        given(midTermForecastPort.fetch(location, d4Date)).willThrow(new RuntimeException("boom"));
        given(uvIndexPort.fetch(location)).willReturn(5);
        DetailSources result = fetcher.fetchDetail(location);
        assertNull(result.getMidTermD4());
        assertThat(result.getPartial().getMissing()).containsExactly("forecast.D4");
        assertTrue(result.getPartial().getDegraded());
    }

    @Test
    void todayRange가_null이면_missing에_todayMinMax가_담긴다() {
        given(currentObservationPort.fetch(location)).willReturn(currentObservation());
        given(shortTermForecastPort.fetchLatest(location)).willReturn(shortTermForecast(false));
        given(shortTermForecastPort.fetchTodayRange(location)).willReturn(null);
        HomeSources result = fetcher.fetchHome(location);
        assertNull(result.getTodayRange());
        assertThat(result.getPartial().getMissing()).containsExactly("todayMinMax");
    }

    @Test
    void latest가_null이면_missing에_forecast가_담긴다() {
        given(currentObservationPort.fetch(location)).willReturn(currentObservation());
        given(shortTermForecastPort.fetchLatest(location)).willReturn(null);
        given(shortTermForecastPort.fetchTodayRange(location)).willReturn(dailyForecast(today));
        HomeSources result = fetcher.fetchHome(location);
        assertNull(result.getLatest());
        assertThat(result.getPartial().getMissing()).containsExactly("forecast");
    }

    @Test
    void 전부_성공하면_missing이_비고_degraded는_false다() {
        given(currentObservationPort.fetch(location)).willReturn(currentObservation());
        given(shortTermForecastPort.fetchLatest(location)).willReturn(shortTermForecast(true));
        given(shortTermForecastPort.fetchTodayRange(location)).willReturn(dailyForecast(today));
        given(uvIndexPort.fetch(location)).willReturn(5);
        DetailSources result = fetcher.fetchDetail(location);
        assertThat(result.getPartial().getMissing()).isEmpty();
        assertFalse(result.getPartial().getDegraded());
    }

    @Test
    void fetchHome은_자외선과_중기예보_포트를_호출하지_않는다() {
        given(currentObservationPort.fetch(location)).willReturn(currentObservation());
        given(shortTermForecastPort.fetchLatest(location)).willReturn(shortTermForecast(false));
        given(shortTermForecastPort.fetchTodayRange(location)).willReturn(dailyForecast(today));
        fetcher.fetchHome(location);
        verifyNoInteractions(uvIndexPort, midTermForecastPort);
    }

    @Test
    void 실황_실패라도_성공한_선택값은_유지하고_반쪽_D4는_missing이다() {
        given(currentObservationPort.fetch(location)).willThrow(new IllegalStateException("secret omitted"));
        ShortTermForecast latest = new ShortTermForecast(WeatherCondition.RAIN, 70, List.of(new DailyForecast(d4Date, WeatherCondition.CLOUDY, 10, null)));
        given(shortTermForecastPort.fetchLatest(location)).willReturn(latest);
        given(shortTermForecastPort.fetchTodayRange(location)).willReturn(dailyForecast(today));
        given(uvIndexPort.fetch(location)).willReturn(9);
        DetailSources result = fetcher.fetchDetail(location);
        assertSame(latest, result.getLatest());
        assertEquals(25, result.getTodayRange().getMinTemperature());
        assertEquals(9, result.getUvIndex());
        assertEquals(20, result.getMidTermD4().getMaxTemperature());
        assertThat(result.getPartial().getMissing()).containsExactly("current", "forecast.D4");
    }

    @Test
    void Error는_흡수하지_않고_join에서_CompletionException으로_전파한다() {
        AssertionError error = new AssertionError("fatal");
        given(currentObservationPort.fetch(location)).willThrow(error);
        CompletionException thrown = assertThrows(CompletionException.class, () -> fetcher.fetchHome(location));
        assertSame(error, thrown.getCause());
    }

    @Test
    void 다섯_소스는_가상스레드에서_동시에_시작된다() throws Exception {
        CountDownLatch started = new CountDownLatch(5);
        CountDownLatch release = new CountDownLatch(1);
        List<Boolean> virtual = new CopyOnWriteArrayList<>();
        org.mockito.stubbing.Answer<Object> answer = invocation -> {
            virtual.add(Thread.currentThread().isVirtual());
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test release timeout");
            return invocation.getMethod().getReturnType() == CurrentObservation.class ? currentObservation() : null;
        };
        given(currentObservationPort.fetch(location)).willAnswer(answer);
        given(shortTermForecastPort.fetchLatest(location)).willAnswer(answer);
        given(shortTermForecastPort.fetchTodayRange(location)).willAnswer(answer);
        given(midTermForecastPort.fetch(location, d4Date)).willAnswer(answer);
        given(uvIndexPort.fetch(location)).willAnswer(answer);
        CompletableFuture<DetailSources> result = CompletableFuture.supplyAsync(() -> fetcher.fetchDetail(location));
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertFalse(result.isDone());
            assertThat(virtual).containsExactly(true, true, true, true, true);
        } finally {
            release.countDown();
        }
        assertEquals(24, result.get(5, TimeUnit.SECONDS).getCurrent().getTemperature());
    }

    private CurrentObservation currentObservation() { return new CurrentObservation(24, null, LocalDateTime.now(clock), 85, 1.7, 26); }
    private ShortTermForecast shortTermForecast(boolean includeD4) {
        List<DailyForecast> forecasts = new ArrayList<>(List.of(dailyForecast(today)));
        if (includeD4) forecasts.add(dailyForecast(d4Date));
        return new ShortTermForecast(WeatherCondition.CLEAR, 0, forecasts);
    }
    private DailyForecast dailyForecast(LocalDate date) { return new DailyForecast(date, WeatherCondition.CLEAR, 25, 29); }
}
