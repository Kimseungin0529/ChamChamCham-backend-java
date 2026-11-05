package com.chamchamcham.application.weather;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.member.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FarmWeatherServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final LocalDate today = LocalDate.of(2026, 7, 15);
    @Mock private FarmRepository farmRepository;
    @Mock private WeatherParallelFetcher weatherParallelFetcher;
    @Mock private ShortTermForecastPort shortTermForecastPort;
    @Mock private HistoricalObservationPort historicalObservationPort;
    private FarmWeatherService service;
    private Member member;
    private Farm farm;

    @BeforeEach
    void setUp() {
        member = new Member(memberId, "member@example.com", null);
        farm = new Farm(farmId, member, "황기밭", "서울 중구 세종대로 110");
        farm.setLatitude(37.5);
        farm.setLongitude(128.1);
        farm.setPnu("1114010300100010000");
        service = new FarmWeatherService(farmRepository, weatherParallelFetcher, shortTermForecastPort, historicalObservationPort, fixedClock);
    }

    @Test
    void farmId_없으면_처음_등록한_농지를_사용한다() {
        given(farmRepository.findFirstByOwnerIdOrderByCreatedAtAsc(memberId)).willReturn(farm);
        given(weatherParallelFetcher.fetchHome(location())).willReturn(homeSources());
        assertEquals(farmId, service.getHome(memberId, null).getFarmId());
    }

    @Test
    void farmId_있으면_소유_농지를_사용한다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(weatherParallelFetcher.fetchHome(location())).willReturn(homeSources());
        assertEquals(farmId, service.getHome(memberId, farmId).getFarmId());
    }

    @Test
    void 농지가_없으면_FARM_NOT_FOUND() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(null);
        assertError(ErrorCode.FARM_NOT_FOUND, () -> service.getHome(memberId, farmId));
    }

    @Test
    void 좌표가_없으면_WEATHER_LOCATION_REQUIRED() {
        Farm noCoordinate = new Farm(farmId, member, "황기밭", "서울 중구 세종대로 110");
        noCoordinate.setLatitude(null);
        noCoordinate.setLongitude(37.5);
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(noCoordinate);
        assertError(ErrorCode.WEATHER_LOCATION_REQUIRED, () -> service.getHome(memberId, farmId));
    }

    @Test
    void 강수형태가_있으면_하늘상태보다_우선한다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(weatherParallelFetcher.fetchHome(location())).willReturn(new HomeSources(currentObservation(WeatherCondition.RAIN), shortTermForecast(WeatherCondition.CLEAR), dailyForecast(today), partial()));
        assertEquals(WeatherCondition.RAIN, service.getHome(memberId, farmId).getCondition());
    }

    @Test
    void 강수형태가_없으면_최신_단기예보의_하늘상태를_쓴다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(weatherParallelFetcher.fetchHome(location())).willReturn(new HomeSources(currentObservation(null), shortTermForecast(WeatherCondition.PARTLY_CLOUDY), dailyForecast(today), partial()));
        assertEquals(WeatherCondition.PARTLY_CLOUDY, service.getHome(memberId, farmId).getCondition());
    }

    @Test
    void D4가_최신_단기예보에_있으면_중기예보_대신_그것을_쓴다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        DailyForecast latestD4 = new DailyForecast(today.plusDays(4), WeatherCondition.CLEAR, 20, 28);
        DailyForecast midD4 = new DailyForecast(today.plusDays(4), WeatherCondition.CLOUDY, 10, 15);
        given(weatherParallelFetcher.fetchDetail(location())).willReturn(detailSources(List.of(dailyForecast(today.plusDays(1)), dailyForecast(today.plusDays(2)), dailyForecast(today.plusDays(3)), latestD4), midD4));
        assertThat(service.getDetail(memberId, farmId).getForecast()).filteredOn(day -> day.getDate().equals(today.plusDays(4))).containsExactly(latestD4);
    }

    @Test
    void D4가_최신_단기예보에_있어도_온도가_없으면_중기예보를_쓴다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        DailyForecast sliver = new DailyForecast(today.plusDays(4), WeatherCondition.PARTLY_CLOUDY, null, null);
        DailyForecast mid = new DailyForecast(today.plusDays(4), WeatherCondition.CLOUDY, 24, 30);
        given(weatherParallelFetcher.fetchDetail(location())).willReturn(detailSources(List.of(dailyForecast(today.plusDays(1)), dailyForecast(today.plusDays(2)), dailyForecast(today.plusDays(3)), sliver), mid));
        List<DailyForecast> result = service.getDetail(memberId, farmId).getForecast();
        assertThat(result).hasSize(5);
        assertThat(result).filteredOn(day -> day.getDate().equals(today.plusDays(4))).containsExactly(mid);
    }

    @Test
    void D4가_최신_단기예보에_없으면_중기예보를_쓴다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        DailyForecast mid = new DailyForecast(today.plusDays(4), WeatherCondition.CLOUDY, 10, 15);
        given(weatherParallelFetcher.fetchDetail(location())).willReturn(detailSources(List.of(dailyForecast(today.plusDays(1)), dailyForecast(today.plusDays(2)), dailyForecast(today.plusDays(3))), mid));
        assertThat(service.getDetail(memberId, farmId).getForecast()).filteredOn(day -> day.getDate().equals(today.plusDays(4))).containsExactly(mid);
    }

    @Test
    void 일부_날짜가_없으면_그_날짜를_빼고_5일_미만으로_반환한다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(weatherParallelFetcher.fetchDetail(location())).willReturn(new DetailSources(currentObservation(null), new ShortTermForecast(WeatherCondition.CLEAR, 10, List.of(dailyForecast(today.plusDays(1)))), dailyForecast(today), null, null, partial()));
        assertThat(service.getDetail(memberId, farmId).getForecast()).extracting(DailyForecast::getDate).containsExactly(today, today.plusDays(1));
    }

    @Test
    void getDaily_미래_날짜는_WEATHER_DATE_IN_FUTURE() {
        assertError(ErrorCode.WEATHER_DATE_IN_FUTURE, () -> service.getDaily(memberId, farmId, today.plusDays(1)));
    }

    @Test
    void getDaily_1년보다_과거는_WEATHER_DATE_TOO_OLD() {
        assertError(ErrorCode.WEATHER_DATE_TOO_OLD, () -> service.getDaily(memberId, farmId, today.minusYears(1).minusDays(1)));
    }

    @Test
    void getDaily_오늘은_fetchTodayRange를_쓰고_ASOS는_부르지_않는다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(shortTermForecastPort.fetchTodayRange(location())).willReturn(dailyForecast(today));
        DailyWeather result = service.getDaily(memberId, farmId, today);
        assertEquals(25, result.getMinTemperature());
        assertEquals(29, result.getMaxTemperature());
        verifyNoInteractions(historicalObservationPort);
    }

    @Test
    void getDaily_오늘_min_max_중_하나라도_없으면_WEATHER_DAILY_DATA_NOT_FOUND() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(shortTermForecastPort.fetchTodayRange(location())).willReturn(new DailyForecast(today, WeatherCondition.CLEAR, null, 29));
        assertError(ErrorCode.WEATHER_DAILY_DATA_NOT_FOUND, () -> service.getDaily(memberId, farmId, today));
    }

    @Test
    void getDaily_과거는_ASOS를_쓰고_없으면_WEATHER_DAILY_DATA_NOT_FOUND() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(historicalObservationPort.fetch(location(), today.minusDays(1))).willReturn(null);
        assertError(ErrorCode.WEATHER_DAILY_DATA_NOT_FOUND, () -> service.getDaily(memberId, farmId, today.minusDays(1)));
    }

    @Test
    void getDaily_과거_정상_조회() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        DailyWeather historical = new DailyWeather(today.minusDays(3), WeatherCondition.CLOUDY, 20, 26);
        given(historicalObservationPort.fetch(location(), today.minusDays(3))).willReturn(historical);
        assertEquals(historical, service.getDaily(memberId, farmId, today.minusDays(3)));
    }

    @Test
    void getDaily_오늘_provider_장애면_흐림_20도_폴백을_반환한다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(shortTermForecastPort.fetchTodayRange(location())).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        assertFallback(service.getDaily(memberId, farmId, today));
    }

    @Test
    void getDaily_과거_provider_장애면_흐림_20도_폴백을_반환한다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(historicalObservationPort.fetch(location(), today.minusDays(2))).willThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        assertFallback(service.getDaily(memberId, farmId, today.minusDays(2)));
    }

    @Test
    void getDaily_정확히_1년전은_포함하고_검증실패는_농지를_조회하지_않는다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        LocalDate date = LocalDate.of(2025, 7, 15);
        DailyWeather historical = new DailyWeather(date, WeatherCondition.SNOW, -5, 0);
        given(historicalObservationPort.fetch(location(), date)).willReturn(historical);
        assertSame(historical, service.getDaily(memberId, farmId, date));
    }

    @Test
    void getDaily_오늘_예보없음과_최고없음도_404이고_일반예외는_전파한다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(shortTermForecastPort.fetchTodayRange(location())).willReturn(null)
            .willReturn(new DailyForecast(today, WeatherCondition.CLEAR, 10, null))
            .willThrow(new IllegalStateException("provider bug"));
        assertError(ErrorCode.WEATHER_DAILY_DATA_NOT_FOUND, () -> service.getDaily(memberId, farmId, today));
        assertError(ErrorCode.WEATHER_DAILY_DATA_NOT_FOUND, () -> service.getDaily(memberId, farmId, today));
        assertThrows(IllegalStateException.class, () -> service.getDaily(memberId, farmId, today));
    }

    @Test
    void 강수와_최신예보가_모두없으면_UNKNOWN이고_날짜검증은_농지보다_먼저다() {
        assertError(ErrorCode.WEATHER_DATE_IN_FUTURE, () -> service.getDaily(memberId, farmId, today.plusDays(1)));
        verifyNoInteractions(farmRepository, weatherParallelFetcher, shortTermForecastPort, historicalObservationPort);
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(weatherParallelFetcher.fetchHome(location())).willReturn(new HomeSources(currentObservation(null), null, null, partial()));
        assertEquals(WeatherCondition.UNKNOWN, service.getHome(memberId, farmId).getCondition());
    }

    @Test
    void 예보는_날짜정렬과_최초선택을_유지하고_결과크기에_따라_가변성이_다르다() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(weatherParallelFetcher.fetchDetail(location())).willReturn(new DetailSources(currentObservation(null), null, null, null, null, partial()));
        List<DailyForecast> empty = service.getDetail(memberId, farmId).getForecast();
        assertTrue(empty.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> empty.add(dailyForecast(today)));
        given(weatherParallelFetcher.fetchDetail(location())).willReturn(new DetailSources(currentObservation(null), null, dailyForecast(today), null, null, partial()));
        List<DailyForecast> single = service.getDetail(memberId, farmId).getForecast();
        assertThat(single).extracting(DailyForecast::getDate).containsExactly(LocalDate.of(2026, 7, 15));
        assertThrows(UnsupportedOperationException.class, () -> single.set(0, dailyForecast(today.plusDays(1))));
        DailyForecast first = new DailyForecast(today.plusDays(1), WeatherCondition.RAIN, 1, 2);
        DailyForecast second = new DailyForecast(today.plusDays(1), WeatherCondition.CLEAR, 3, 4);
        given(weatherParallelFetcher.fetchDetail(location())).willReturn(detailSources(List.of(dailyForecast(today.plusDays(3)), first, second, dailyForecast(today.plusDays(2))), dailyForecast(today.plusDays(4))));
        List<DailyForecast> multiple = service.getDetail(memberId, farmId).getForecast();
        assertThat(multiple).extracting(DailyForecast::getDate).containsExactly(today, today.plusDays(1), today.plusDays(2), today.plusDays(3), today.plusDays(4));
        assertSame(first, multiple.get(1));
        multiple.set(0, second);
        assertThrows(UnsupportedOperationException.class, () -> multiple.add(first));
        assertThrows(UnsupportedOperationException.class, () -> multiple.remove(0));
        assertEquals(5, multiple.size());
    }

    @Test
    void 모델은_입력목록_alias와_파생값_및_숫자동등성을_보존한다() {
        List<String> missing = new ArrayList<>();
        PartialFailure partial = new PartialFailure(missing);
        assertSame(missing, partial.getMissing());
        assertFalse(partial.getDegraded());
        missing.add("uvIndex");
        assertTrue(partial.getDegraded());
        assertEquals(new PartialFailure(List.of("uvIndex")), partial);
        List<DailyForecast> days = new ArrayList<>();
        ShortTermForecast forecast = new ShortTermForecast(WeatherCondition.CLEAR, null, days);
        assertSame(days, forecast.getDailyForecasts());
        days.add(dailyForecast(today));
        assertEquals(new ShortTermForecast(WeatherCondition.CLEAR, null, List.of(dailyForecast(today))), forecast);
        assertEquals(new WeatherLocation(Double.NaN, 0.0, "", null), new WeatherLocation(Double.NaN, 0.0, "", null));
        assertNotEquals(new WeatherLocation(0.0, 0.0, "", null), new WeatherLocation(-0.0, 0.0, "", null));
        assertFalse(new DailyForecast(today, WeatherCondition.CLEAR, 1, null).getHasTemperatureRange());
        assertTrue(new DailyForecast(today, WeatherCondition.CLEAR, 1, 2).getHasTemperatureRange());
    }

    private void assertFallback(DailyWeather result) {
        assertEquals(WeatherCondition.CLOUDY, result.getCondition());
        assertEquals(20, result.getMinTemperature());
        assertEquals(20, result.getMaxTemperature());
    }
    private void assertError(ErrorCode expected, org.junit.jupiter.api.function.Executable call) {
        assertEquals(expected, assertThrows(BusinessException.class, call).getErrorCode());
    }
    private WeatherLocation location() { return new WeatherLocation(37.5, 128.1, "서울 중구 세종대로 110", "1114010300100010000"); }
    private CurrentObservation currentObservation(WeatherCondition precipitationType) { return new CurrentObservation(24, precipitationType, LocalDateTime.of(2026, 7, 15, 21, 0), 85, 1.7, 26); }
    private ShortTermForecast shortTermForecast(WeatherCondition sky) { return new ShortTermForecast(sky, 0, Collections.emptyList()); }
    private DailyForecast dailyForecast(LocalDate date) { return new DailyForecast(date, WeatherCondition.CLEAR, 25, 29); }
    private PartialFailure partial() { return new PartialFailure(new ArrayList<>()); }
    private HomeSources homeSources() { return new HomeSources(currentObservation(null), shortTermForecast(WeatherCondition.CLEAR), dailyForecast(today), partial()); }
    private DetailSources detailSources(List<DailyForecast> days, DailyForecast mid) { return new DetailSources(currentObservation(null), new ShortTermForecast(WeatherCondition.CLEAR, 10, days), dailyForecast(today), mid, 5, partial()); }
}
