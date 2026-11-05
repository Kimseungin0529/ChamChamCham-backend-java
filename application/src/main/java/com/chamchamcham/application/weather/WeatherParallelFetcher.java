package com.chamchamcham.application.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** 날씨 소스를 전용 가상 스레드에서 병렬 조회하고 원본 결측과 폴백을 조립한다. */
@Component
public class WeatherParallelFetcher implements DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(WeatherParallelFetcher.class);
    private final CurrentObservationPort currentObservationPort;
    private final ShortTermForecastPort shortTermForecastPort;
    private final MidTermForecastPort midTermForecastPort;
    private final UvIndexPort uvIndexPort;
    private final Clock clock;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public WeatherParallelFetcher(CurrentObservationPort currentObservationPort, ShortTermForecastPort shortTermForecastPort,
                                  MidTermForecastPort midTermForecastPort, UvIndexPort uvIndexPort, Clock clock) {
        this.currentObservationPort = Objects.requireNonNull(currentObservationPort, "currentObservationPort");
        this.shortTermForecastPort = Objects.requireNonNull(shortTermForecastPort, "shortTermForecastPort");
        this.midTermForecastPort = Objects.requireNonNull(midTermForecastPort, "midTermForecastPort");
        this.uvIndexPort = Objects.requireNonNull(uvIndexPort, "uvIndexPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @NonNull
    public DetailSources fetchDetail(@NonNull WeatherLocation location) {
        Objects.requireNonNull(location, "location");
        CompletableFuture<CurrentObservation> currentFuture = supplyCurrent(location);
        CompletableFuture<ShortTermForecast> latestFuture = supplyLatest(location);
        CompletableFuture<DailyForecast> todayRangeFuture = supplyTodayRange(location);
        LocalDate today = LocalDate.now(clock);
        LocalDate d4Date = today.plusDays(4);
        CompletableFuture<DailyForecast> midFuture = supplyMidTerm(location, d4Date);
        CompletableFuture<Integer> uvFuture = supplyUvIndex(location);

        CurrentObservation currentRaw = currentFuture.join();
        ShortTermForecast latestRaw = latestFuture.join();
        DailyForecast todayRangeRaw = todayRangeFuture.join();
        DailyForecast midTermD4Raw = midFuture.join();
        Integer uvIndexRaw = uvFuture.join();

        boolean d4FromLatest = latestRaw != null && latestRaw.getDailyForecasts().stream()
            .anyMatch(day -> day.getDate().equals(d4Date) && day.getHasTemperatureRange());
        PartialFailure partial = PartialFailure.of(
            Map.entry("current", currentRaw == null),
            Map.entry("todayMinMax", todayRangeRaw == null),
            Map.entry("forecast", latestRaw == null),
            Map.entry("forecast.D4", !d4FromLatest && midTermD4Raw == null),
            Map.entry("uvIndex", uvIndexRaw == null));
        boolean providerDown = currentRaw == null;
        return new DetailSources(
            currentRaw != null ? currentRaw : WeatherFallback.currentObservation(clock),
            latestRaw != null ? latestRaw : fallbackIfProviderDown(providerDown, () -> WeatherFallback.shortTermForecast(today)),
            todayRangeRaw != null ? todayRangeRaw : fallbackIfProviderDown(providerDown, () -> WeatherFallback.dailyForecast(today)),
            midTermD4Raw != null ? midTermD4Raw : fallbackIfProviderDown(providerDown, () -> WeatherFallback.dailyForecast(d4Date)),
            uvIndexRaw != null ? uvIndexRaw : fallbackIfProviderDown(providerDown, () -> WeatherFallback.UV_INDEX), partial);
    }

    @NonNull
    public HomeSources fetchHome(@NonNull WeatherLocation location) {
        Objects.requireNonNull(location, "location");
        CompletableFuture<CurrentObservation> currentFuture = supplyCurrent(location);
        CompletableFuture<ShortTermForecast> latestFuture = supplyLatest(location);
        CompletableFuture<DailyForecast> todayRangeFuture = supplyTodayRange(location);
        CurrentObservation currentRaw = currentFuture.join();
        ShortTermForecast latestRaw = latestFuture.join();
        DailyForecast todayRangeRaw = todayRangeFuture.join();
        PartialFailure partial = PartialFailure.of(
            Map.entry("current", currentRaw == null),
            Map.entry("todayMinMax", todayRangeRaw == null),
            Map.entry("forecast", latestRaw == null));
        boolean providerDown = currentRaw == null;
        LocalDate today = LocalDate.now(clock);
        return new HomeSources(
            currentRaw != null ? currentRaw : WeatherFallback.currentObservation(clock),
            latestRaw != null ? latestRaw : fallbackIfProviderDown(providerDown, () -> WeatherFallback.shortTermForecast(today)),
            todayRangeRaw != null ? todayRangeRaw : fallbackIfProviderDown(providerDown, () -> WeatherFallback.dailyForecast(today)), partial);
    }

    private <T> T fallbackIfProviderDown(boolean providerDown, Supplier<T> fallback) {
        return providerDown ? fallback.get() : null;
    }

    private CompletableFuture<CurrentObservation> supplyCurrent(WeatherLocation location) {
        return CompletableFuture.supplyAsync(() -> fetchOptional("current", () -> currentObservationPort.fetch(location)), executor);
    }

    private CompletableFuture<ShortTermForecast> supplyLatest(WeatherLocation location) {
        return CompletableFuture.supplyAsync(() -> fetchOptional("shortTermForecast.latest", () -> shortTermForecastPort.fetchLatest(location)), executor);
    }

    private CompletableFuture<DailyForecast> supplyTodayRange(WeatherLocation location) {
        return CompletableFuture.supplyAsync(() -> fetchOptional("shortTermForecast.todayRange", () -> shortTermForecastPort.fetchTodayRange(location)), executor);
    }

    private CompletableFuture<DailyForecast> supplyMidTerm(WeatherLocation location, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> fetchOptional("midTermForecast", () -> midTermForecastPort.fetch(location, date)), executor);
    }

    private CompletableFuture<Integer> supplyUvIndex(WeatherLocation location) {
        return CompletableFuture.supplyAsync(() -> fetchOptional("uvIndex", () -> uvIndexPort.fetch(location)), executor);
    }

    private <T> T fetchOptional(String sourceName, Supplier<T> block) {
        try {
            return block.get();
        } catch (Exception exception) {
            // 인증 정보가 섞일 수 있는 예외 메시지 대신 소스와 예외 종류만 기록한다.
            logger.warn("weather optional source failed: {} ({})", sourceName, exception.getClass().getSimpleName());
            return null;
        }
    }

    @Override
    public void destroy() { executor.shutdown(); }
}
