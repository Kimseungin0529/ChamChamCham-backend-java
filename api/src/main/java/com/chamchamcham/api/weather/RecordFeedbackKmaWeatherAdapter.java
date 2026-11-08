package com.chamchamcham.api.weather;

import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackCurrentWeather;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackForecastDay;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackLiveWeather;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackWeatherPort;
import com.chamchamcham.application.weather.CurrentObservation;
import com.chamchamcham.application.weather.CurrentObservationPort;
import com.chamchamcham.application.weather.WeatherCondition;
import com.chamchamcham.application.weather.WeatherLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 기록 피드백에 필요한 일별 집계와 위험 신호를 실황·단기예보로 구성한다. */
@Component
public class RecordFeedbackKmaWeatherAdapter implements RecordFeedbackWeatherPort {
    private static final String KMA_SHORT_TERM_SOURCE = "KMA_SHORT_TERM";
    private static final DateTimeFormatter FORECAST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Pattern EXACT_RAINFALL_PATTERN = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:mm)?\\s*$");
    private static final BigDecimal HEAVY_RAIN_THRESHOLD_MM = new BigDecimal("20");
    private static final BigDecimal HIGH_HUMIDITY_THRESHOLD_PCT = new BigDecimal("85");
    private static final BigDecimal HOT_THRESHOLD_C = new BigDecimal("30");
    private static final BigDecimal STRONG_WIND_THRESHOLD_MS = new BigDecimal("8");
    private final CurrentObservationPort currentObservationPort;
    private final KmaApiClient kmaApiClient;
    private final KmaBaseTime kmaBaseTime;
    private final KmaProperties properties;
    private final Clock clock;

    public RecordFeedbackKmaWeatherAdapter(CurrentObservationPort currentObservationPort, KmaApiClient kmaApiClient,
                                          KmaBaseTime kmaBaseTime, KmaProperties properties, Clock clock) {
        this.currentObservationPort = Objects.requireNonNull(currentObservationPort, "currentObservationPort");
        this.kmaApiClient = Objects.requireNonNull(kmaApiClient, "kmaApiClient");
        this.kmaBaseTime = Objects.requireNonNull(kmaBaseTime, "kmaBaseTime");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public RecordFeedbackLiveWeather fetch(double latitude, double longitude, int limitDays) {
        if (limitDays < 1 || limitDays > 7) throw new IllegalArgumentException("limitDays must be between 1 and 7");
        CurrentObservation current = currentObservationPort.fetch(new WeatherLocation(latitude, longitude, "", null));
        List<VilageFcstItemDto> items = fetchVilageFcstItems(latitude, longitude);
        LocalDate today = LocalDate.now(clock);
        WeatherCondition currentSky = current.getPrecipitationType();
        if (currentSky == null) currentSky = nearestSky(items);
        if (currentSky == null) currentSky = WeatherCondition.UNKNOWN;
        return new RecordFeedbackLiveWeather(
            new RecordFeedbackCurrentWeather(current.getTemperature(), currentSky.getText(), current.getObservedAt()),
            aggregateForecastDays(items, today, limitDays), KMA_SHORT_TERM_SOURCE);
    }

    private List<VilageFcstItemDto> fetchVilageFcstItems(double latitude, double longitude) {
        KmaGrid grid = GeoToGridConverter.convert(latitude, longitude);
        KmaBaseDateTime base = kmaBaseTime.resolveLatest();
        Map<String, String> params = new LinkedHashMap<>();
        // 기록 피드백은 7일치 전 카테고리를 훑으므로 기존처럼 2000행을 요청한다.
        params.put("numOfRows", "2000");
        params.put("base_date", base.getBaseDate());
        params.put("base_time", base.getBaseTime());
        params.put("nx", Integer.toString(grid.getNx()));
        params.put("ny", Integer.toString(grid.getNy()));
        return kmaApiClient.getItems(properties.getBaseUrl().getVilageFcst(), "getVilageFcst", params, VilageFcstItemDto.class);
    }

    private WeatherCondition nearestSky(List<VilageFcstItemDto> items) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nearest = items.stream().map(this::parseFcstDateTime).filter(Objects::nonNull).distinct()
            .min(Comparator.comparing(slot -> Duration.between(now, slot).abs())).orElse(null);
        if (nearest == null) return null;
        String sky = null;
        for (VilageFcstItemDto item : items) {
            if ("SKY".equals(item.category()) && nearest.equals(parseFcstDateTime(item))) { sky = item.fcstValue(); break; }
        }
        WeatherCondition condition = SkyPtyConditionMapper.of(sky, null);
        return condition == WeatherCondition.UNKNOWN ? null : condition;
    }

    private LocalDateTime parseFcstDateTime(VilageFcstItemDto item) {
        if (item.fcstDate() == null || item.fcstTime() == null) return null;
        try { return LocalDateTime.parse(item.fcstDate() + item.fcstTime(), DATE_TIME_FORMAT); }
        catch (DateTimeParseException exception) { return null; }
    }

    private List<RecordFeedbackForecastDay> aggregateForecastDays(List<VilageFcstItemDto> items, LocalDate today, int limitDays) {
        Map<LocalDate, List<VilageFcstItemDto>> byDate = new TreeMap<>();
        for (VilageFcstItemDto item : items) {
            LocalDate date = item.fcstDate() == null ? null : parseForecastDate(item.fcstDate());
            if (date != null && !date.isBefore(today)) byDate.computeIfAbsent(date, key -> new ArrayList<>()).add(item);
        }
        List<RecordFeedbackForecastDay> days = new ArrayList<>(Math.min(byDate.size(), limitDays));
        for (var entry : byDate.entrySet()) {
            if (days.size() == limitDays) break;
            days.add(aggregateForecastDay(entry.getKey(), entry.getValue()));
        }
        return days;
    }

    private RecordFeedbackForecastDay aggregateForecastDay(LocalDate date, List<VilageFcstItemDto> items) {
        BigDecimal min = null, max = null, rainfall = null;
        boolean hasPrecipitationType = false;
        for (VilageFcstItemDto item : items) {
            if ("TMP".equals(item.category()) || "TMN".equals(item.category()) || "TMX".equals(item.category())) {
                BigDecimal value = decimal(item.fcstValue());
                if (value != null) {
                    if (min == null || value.compareTo(min) < 0) min = value;
                    if (max == null || value.compareTo(max) > 0) max = value;
                }
            }
            if ("PCP".equals(item.category())) {
                BigDecimal value = exactRainfall(item.fcstValue());
                if (value != null && (rainfall == null || value.compareTo(rainfall) > 0)) rainfall = value;
            }
            if ("PTY".equals(item.category())) {
                Integer value = KmaValueParser.integer(item.fcstValue());
                if (value != null && value != 0) hasPrecipitationType = true;
            }
        }
        Integer rainProbability = maxIntValue(items, "POP");
        BigDecimal humidity = maxDecimalValue(items, "REH");
        BigDecimal windSpeed = maxDecimalValue(items, "WSD");
        List<String> flags = new ArrayList<>();
        if (hasPrecipitationType || (rainProbability == null ? 0 : rainProbability) >= 60) flags.add("RAIN");
        if (rainfall != null && rainfall.compareTo(HEAVY_RAIN_THRESHOLD_MM) >= 0) flags.add("HEAVY_RAIN");
        if (humidity != null && humidity.compareTo(HIGH_HUMIDITY_THRESHOLD_PCT) >= 0) flags.add("HIGH_HUMIDITY");
        if (max != null && max.compareTo(HOT_THRESHOLD_C) >= 0) flags.add("HOT");
        if (windSpeed != null && windSpeed.compareTo(STRONG_WIND_THRESHOLD_MS) >= 0) flags.add("STRONG_WIND");
        return new RecordFeedbackForecastDay(date, rainfall, rainProbability, max, min, humidity, windSpeed, flags);
    }

    private LocalDate parseForecastDate(String value) {
        try { return LocalDate.parse(value, FORECAST_DATE_FORMAT); }
        catch (Exception exception) { return null; }
    }

    private Integer maxIntValue(List<VilageFcstItemDto> items, String category) {
        Integer max = null;
        for (VilageFcstItemDto item : items) {
            if (!category.equals(item.category())) continue;
            Integer value = KmaValueParser.integer(item.fcstValue());
            if (value != null && (max == null || value > max)) max = value;
        }
        return max;
    }

    private BigDecimal maxDecimalValue(List<VilageFcstItemDto> items, String category) {
        BigDecimal max = null;
        for (VilageFcstItemDto item : items) {
            if (!category.equals(item.category())) continue;
            BigDecimal value = decimal(item.fcstValue());
            if (value != null && (max == null || value.compareTo(max) > 0)) max = value;
        }
        return max;
    }

    private BigDecimal decimal(String value) {
        if (value == null) return null;
        try { return new BigDecimal(value); }
        catch (NumberFormatException exception) { return null; }
    }

    private BigDecimal exactRainfall(String value) {
        if (value == null) return null;
        Matcher matcher = EXACT_RAINFALL_PATTERN.matcher(value);
        return matcher.matches() ? decimal(matcher.group(1)) : null;
    }

    private record VilageFcstItemDto(@JsonProperty("category") String category, @JsonProperty("fcstDate") String fcstDate,
                                    @JsonProperty("fcstTime") String fcstTime, @JsonProperty("fcstValue") String fcstValue) {}
}
