package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.DailyForecast;
import com.chamchamcham.application.weather.ShortTermForecast;
import com.chamchamcham.application.weather.ShortTermForecastPort;
import com.chamchamcham.application.weather.WeatherCondition;
import com.chamchamcham.application.weather.WeatherLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 최신 예보와 당일 최저·최고 전용 발표를 구분한다. 선택 소스의 부재는 null이다. */
@Component
public class KmaShortTermForecastAdapter implements ShortTermForecastPort {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private final KmaApiClient kmaApiClient;
    private final KmaBaseTime kmaBaseTime;
    private final KmaProperties properties;
    private final Clock clock;

    public KmaShortTermForecastAdapter(KmaApiClient kmaApiClient, KmaBaseTime kmaBaseTime, KmaProperties properties, Clock clock) {
        this.kmaApiClient = Objects.requireNonNull(kmaApiClient, "kmaApiClient");
        this.kmaBaseTime = Objects.requireNonNull(kmaBaseTime, "kmaBaseTime");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public ShortTermForecast fetchLatest(WeatherLocation location) {
        List<VilageFcstItemDto> items = fetchItems(location, kmaBaseTime.resolveLatest());
        if (items.isEmpty()) return null;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nearest = items.stream().map(this::parseFcstDateTime).filter(Objects::nonNull).distinct()
            .min(Comparator.comparing(slot -> Duration.between(now, slot).abs())).orElse(null);
        String sky = nearest == null ? null : valueAtSlot(items, nearest, "SKY");
        WeatherCondition currentSky = sky == null ? WeatherCondition.UNKNOWN : SkyPtyConditionMapper.of(sky, null);
        Integer precipitationProbability = nearest == null ? null : KmaValueParser.integer(valueAtSlot(items, nearest, "POP"));
        Map<String, List<VilageFcstItemDto>> byDate = new TreeMap<>();
        for (VilageFcstItemDto item : items) {
            if (item.fcstDate() != null) byDate.computeIfAbsent(item.fcstDate(), key -> new ArrayList<>()).add(item);
        }
        List<DailyForecast> dailyForecasts = new ArrayList<>(byDate.size());
        byDate.forEach((date, dayItems) -> dailyForecasts.add(buildDailyForecast(date, dayItems)));
        return new ShortTermForecast(currentSky, precipitationProbability, dailyForecasts);
    }

    @Override
    public DailyForecast fetchTodayRange(WeatherLocation location) {
        List<VilageFcstItemDto> items = fetchItems(location, kmaBaseTime.resolveTodayRange());
        String today = LocalDate.now(clock).format(DATE_FORMAT);
        List<VilageFcstItemDto> todayItems = items.stream().filter(item -> today.equals(item.fcstDate())).collect(Collectors.toCollection(ArrayList::new));
        return todayItems.isEmpty() ? null : buildDailyForecast(today, todayItems);
    }

    private List<VilageFcstItemDto> fetchItems(WeatherLocation location, KmaBaseDateTime base) {
        KmaGrid grid = GeoToGridConverter.convert(location.getLatitude(), location.getLongitude());
        Map<String, String> params = new LinkedHashMap<>();
        params.put("numOfRows", "1000");
        params.put("base_date", base.getBaseDate());
        params.put("base_time", base.getBaseTime());
        params.put("nx", Integer.toString(grid.getNx()));
        params.put("ny", Integer.toString(grid.getNy()));
        return kmaApiClient.getItems(properties.getBaseUrl().getVilageFcst(), "getVilageFcst", params, VilageFcstItemDto.class);
    }

    // 날짜를 먼저 거르지 않아 23시 발표의 다음날 00시 슬롯도 선택할 수 있다.
    private LocalDateTime parseFcstDateTime(VilageFcstItemDto item) {
        if (item.fcstDate() == null || item.fcstTime() == null) return null;
        try {
            return LocalDateTime.parse(item.fcstDate() + item.fcstTime(), DATE_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String valueAtSlot(List<VilageFcstItemDto> items, LocalDateTime slot, String category) {
        for (VilageFcstItemDto item : items) {
            if (category.equals(item.category()) && slot.equals(parseFcstDateTime(item))) return item.fcstValue();
        }
        return null;
    }

    private DailyForecast buildDailyForecast(String date, List<VilageFcstItemDto> items) {
        // TMN/TMX가 없으면 null. TMP의 min/max로 폴백하지 않는다.
        return new DailyForecast(LocalDate.parse(date, DATE_FORMAT), noonCondition(items),
            KmaValueParser.rounded(valueOfCategory(items, "TMN")), KmaValueParser.rounded(valueOfCategory(items, "TMX")));
    }

    private String valueOfCategory(List<VilageFcstItemDto> items, String category) {
        for (VilageFcstItemDto item : items) if (category.equals(item.category())) return item.fcstValue();
        return null;
    }

    private WeatherCondition noonCondition(List<VilageFcstItemDto> items) {
        String noonTime = items.stream().map(VilageFcstItemDto::fcstTime).filter(Objects::nonNull).distinct()
            .filter(time -> parseTime(time) != null)
            .min(Comparator.comparing(time -> Duration.between(parseTime(time), LocalTime.NOON).abs())).orElse(null);
        if (noonTime == null) return WeatherCondition.UNKNOWN;
        String sky = null, pty = null;
        boolean skyFound = false, ptyFound = false;
        for (VilageFcstItemDto item : items) {
            if (!noonTime.equals(item.fcstTime())) continue;
            if (!skyFound && "SKY".equals(item.category())) { sky = item.fcstValue(); skyFound = true; }
            if (!ptyFound && "PTY".equals(item.category())) { pty = item.fcstValue(); ptyFound = true; }
        }
        return SkyPtyConditionMapper.of(sky, pty);
    }

    private LocalTime parseTime(String time) {
        try { return LocalTime.parse(time, TIME_FORMAT); }
        catch (DateTimeParseException exception) { return null; }
    }

    private record VilageFcstItemDto(@JsonProperty("baseDate") String baseDate, @JsonProperty("baseTime") String baseTime,
                                    @JsonProperty("category") String category, @JsonProperty("fcstDate") String fcstDate,
                                    @JsonProperty("fcstTime") String fcstTime, @JsonProperty("fcstValue") String fcstValue) {}
}
