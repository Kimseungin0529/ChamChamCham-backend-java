package com.chamchamcham.application.weather;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 농지 날씨 유스케이스. 외부 제공자 호출과 병렬 조립은 포트와 fetcher에 맡긴다. */
@Service
@Transactional(readOnly = true)
public class FarmWeatherService {
    private static final Logger logger = LoggerFactory.getLogger(FarmWeatherService.class);
    private final FarmRepository farmRepository;
    private final WeatherParallelFetcher weatherParallelFetcher;
    private final ShortTermForecastPort shortTermForecastPort;
    private final HistoricalObservationPort historicalObservationPort;
    private final Clock clock;

    public FarmWeatherService(FarmRepository farmRepository, WeatherParallelFetcher weatherParallelFetcher,
                              ShortTermForecastPort shortTermForecastPort, HistoricalObservationPort historicalObservationPort,
                              Clock clock) {
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.weatherParallelFetcher = Objects.requireNonNull(weatherParallelFetcher, "weatherParallelFetcher");
        this.shortTermForecastPort = Objects.requireNonNull(shortTermForecastPort, "shortTermForecastPort");
        this.historicalObservationPort = Objects.requireNonNull(historicalObservationPort, "historicalObservationPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @NonNull
    public HomeWeather getHome(@NonNull UUID memberId, @Nullable UUID farmId) {
        Objects.requireNonNull(memberId, "memberId");
        Farm farm = resolveFarm(memberId, farmId);
        HomeSources sources = weatherParallelFetcher.fetchHome(toLocation(farm));
        return new HomeWeather(persistedId(farm), sources.getCurrent().getTemperature(),
            resolveCondition(sources.getCurrent(), sources.getLatest()),
            sources.getTodayRange() == null ? null : sources.getTodayRange().getMinTemperature(),
            sources.getTodayRange() == null ? null : sources.getTodayRange().getMaxTemperature(),
            sources.getCurrent().getObservedAt(), sources.getPartial());
    }

    @NonNull
    public DetailWeather getDetail(@NonNull UUID memberId, @Nullable UUID farmId) {
        Objects.requireNonNull(memberId, "memberId");
        Farm farm = resolveFarm(memberId, farmId);
        DetailSources sources = weatherParallelFetcher.fetchDetail(toLocation(farm));
        LocalDate today = LocalDate.now(clock);
        return new DetailWeather(persistedId(farm), farm.getRoadAddress(), sources.getCurrent().getObservedAt(),
            sources.getCurrent().getTemperature(), sources.getCurrent().getFeelsLikeTemperature(),
            resolveCondition(sources.getCurrent(), sources.getLatest()),
            sources.getTodayRange() == null ? null : sources.getTodayRange().getMinTemperature(),
            sources.getTodayRange() == null ? null : sources.getTodayRange().getMaxTemperature(),
            sources.getCurrent().getHumidity(), sources.getCurrent().getWindSpeed(),
            sources.getLatest() == null ? null : sources.getLatest().getPrecipitationProbability(),
            sources.getUvIndex(), buildForecast(today, sources), sources.getPartial());
    }

    @NonNull
    public DailyWeather getDaily(@NonNull UUID memberId, @Nullable UUID farmId, @NonNull LocalDate date) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(date, "date");
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) throw new BusinessException(ErrorCode.WEATHER_DATE_IN_FUTURE);
        if (date.isBefore(today.minusYears(1))) throw new BusinessException(ErrorCode.WEATHER_DATE_TOO_OLD);
        WeatherLocation location = toLocation(resolveFarm(memberId, farmId));
        try {
            if (date.equals(today)) return todayDailyWeather(location, date);
            DailyWeather historical = historicalObservationPort.fetch(location, date);
            if (historical == null) throw new BusinessException(ErrorCode.WEATHER_DAILY_DATA_NOT_FOUND);
            return historical;
        } catch (BusinessException exception) {
            if (exception.getErrorCode() != ErrorCode.WEATHER_PROVIDER_UNAVAILABLE) throw exception;
            logger.warn("weather daily fallback applied (provider unavailable) date={}", date);
            return WeatherFallback.dailyWeather(date);
        }
    }

    private DailyWeather todayDailyWeather(WeatherLocation location, LocalDate date) {
        DailyForecast forecast = shortTermForecastPort.fetchTodayRange(location);
        if (forecast == null || forecast.getMinTemperature() == null || forecast.getMaxTemperature() == null) {
            throw new BusinessException(ErrorCode.WEATHER_DAILY_DATA_NOT_FOUND);
        }
        return new DailyWeather(date, forecast.getCondition(), forecast.getMinTemperature(), forecast.getMaxTemperature());
    }

    // 실황에는 SKY가 없으므로 강수 실측이 없을 때만 최신 단기예보 하늘 상태를 사용한다.
    private WeatherCondition resolveCondition(CurrentObservation current, ShortTermForecast latest) {
        if (current.getPrecipitationType() != null) return current.getPrecipitationType();
        return latest == null ? WeatherCondition.UNKNOWN : latest.getCurrentSky();
    }

    private List<DailyForecast> buildForecast(LocalDate today, DetailSources sources) {
        List<DailyForecast> days = new ArrayList<>();
        if (sources.getTodayRange() != null) days.add(sources.getTodayRange());
        for (int offset = 1; offset <= 3; offset++) {
            LocalDate date = today.plusDays(offset);
            if (sources.getLatest() != null) {
                sources.getLatest().getDailyForecasts().stream().filter(day -> day.getDate().equals(date))
                    .findFirst().ifPresent(days::add);
            }
        }
        LocalDate d4Date = today.plusDays(4);
        DailyForecast d4 = sources.getLatest() == null ? null : sources.getLatest().getDailyForecasts().stream()
            .filter(day -> day.getDate().equals(d4Date) && day.getHasTemperatureRange()).findFirst().orElse(null);
        if (d4 == null) d4 = sources.getMidTermD4();
        if (d4 != null) days.add(d4);
        // Kotlin sortedBy: 빈/단일 목록은 불변, 다중 목록은 set만 허용하는 배열 기반 목록이다.
        if (days.isEmpty()) return Collections.emptyList();
        if (days.size() == 1) return Collections.singletonList(days.get(0));
        DailyForecast[] sorted = days.toArray(DailyForecast[]::new);
        Arrays.sort(sorted, Comparator.comparing(DailyForecast::getDate));
        return Arrays.asList(sorted);
    }

    private Farm resolveFarm(UUID memberId, UUID farmId) {
        Farm farm = farmId != null ? farmRepository.findByIdAndOwnerId(farmId, memberId)
            : farmRepository.findFirstByOwnerIdOrderByCreatedAtAsc(memberId);
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        return farm;
    }

    private UUID persistedId(Farm farm) {
        if (farm.getId() == null) throw new IllegalArgumentException("Persisted farm id is required");
        return farm.getId();
    }

    private WeatherLocation toLocation(Farm farm) {
        if (farm.getLatitude() == null || farm.getLongitude() == null) {
            throw new BusinessException(ErrorCode.WEATHER_LOCATION_REQUIRED);
        }
        return new WeatherLocation(farm.getLatitude(), farm.getLongitude(), farm.getRoadAddress(), farm.getPnu());
    }
}
