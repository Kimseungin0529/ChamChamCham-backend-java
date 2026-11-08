package com.chamchamcham.api.weather;

import java.util.Objects;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.weather.CurrentObservation;
import com.chamchamcham.application.weather.CurrentObservationPort;
import com.chamchamcham.application.weather.WeatherCondition;
import com.chamchamcham.application.weather.WeatherLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 필수 소스인 초단기실황의 데이터 부재는 WEATHER_002로 변환한다. */
@Component
public class KmaCurrentObservationAdapter implements CurrentObservationPort {
    private static final DateTimeFormatter OBSERVED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private final KmaApiClient kmaApiClient;
    private final KmaBaseTime kmaBaseTime;
    private final KmaProperties properties;

    public KmaCurrentObservationAdapter(KmaApiClient kmaApiClient, KmaBaseTime kmaBaseTime, KmaProperties properties) {
        this.kmaApiClient = Objects.requireNonNull(kmaApiClient, "kmaApiClient");
        this.kmaBaseTime = Objects.requireNonNull(kmaBaseTime, "kmaBaseTime");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public CurrentObservation fetch(WeatherLocation location) {
        KmaGrid grid = GeoToGridConverter.convert(location.getLatitude(), location.getLongitude());
        KmaBaseDateTime base = kmaBaseTime.resolveNcst();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("numOfRows", "100");
        params.put("base_date", base.getBaseDate());
        params.put("base_time", base.getBaseTime());
        params.put("nx", Integer.toString(grid.getNx()));
        params.put("ny", Integer.toString(grid.getNy()));
        List<NcstItemDto> items = kmaApiClient.getItems(properties.getBaseUrl().getVilageFcst(), "getUltraSrtNcst", params, NcstItemDto.class);
        if (items.isEmpty()) throw new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
        Map<String, String> values = new HashMap<>();
        for (NcstItemDto item : items) values.put(item.category(), item.obsrValue());
        Integer temperature = KmaValueParser.rounded(values.get("T1H"));
        if (temperature == null) throw new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
        Integer humidity = KmaValueParser.rounded(values.get("REH"));
        Double windSpeed = KmaValueParser.decimal(values.get("WSD"));
        WeatherCondition precipitationType = resolvePrecipitationType(values.get("PTY"));
        LocalDateTime observedAt = resolveObservedAt(items);
        return new CurrentObservation(temperature, precipitationType, observedAt, humidity, windSpeed,
            FeelsLikeTemperatureCalculator.of(temperature, humidity, windSpeed, observedAt.getMonthValue()));
    }

    private WeatherCondition resolvePrecipitationType(String pty) {
        if (pty == null || pty.equals("0")) return null;
        WeatherCondition condition = SkyPtyConditionMapper.of(null, pty);
        return condition == WeatherCondition.UNKNOWN ? null : condition;
    }

    private LocalDateTime resolveObservedAt(List<NcstItemDto> items) {
        NcstItemDto item = items.get(0);
        if (item.baseDate() == null || item.baseTime() == null) throw new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
        try {
            return LocalDateTime.parse(item.baseDate() + item.baseTime(), OBSERVED_AT_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
        }
    }

    private record NcstItemDto(@JsonProperty("baseDate") String baseDate, @JsonProperty("baseTime") String baseTime,
                               @JsonProperty("category") String category, @JsonProperty("obsrValue") String obsrValue) {}
}
