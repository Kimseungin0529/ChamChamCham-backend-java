package com.chamchamcham.api.weather;

import java.util.Objects;
import com.chamchamcham.application.weather.DailyWeather;
import com.chamchamcham.application.weather.HistoricalObservationPort;
import com.chamchamcham.application.weather.WeatherLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KmaHistoricalObservationAdapter implements HistoricalObservationPort {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final KmaApiClient kmaApiClient;
    private final KmaProperties properties;

    public KmaHistoricalObservationAdapter(KmaApiClient kmaApiClient, KmaProperties properties) {
        this.kmaApiClient = Objects.requireNonNull(kmaApiClient, "kmaApiClient");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public DailyWeather fetch(WeatherLocation location, LocalDate date) {
        AsosStation station = NearestAsosStationResolver.resolve(location.getLatitude(), location.getLongitude());
        String dateText = date.format(DATE_FORMAT);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("dataCd", "ASOS");
        params.put("dateCd", "DAY");
        params.put("startDt", dateText);
        params.put("endDt", dateText);
        params.put("stnIds", station.getId());
        params.put("numOfRows", "10");
        List<AsosDailyItemDto> items = kmaApiClient.getItems(properties.getBaseUrl().getAsos(), "getWthrDataList", params, AsosDailyItemDto.class);
        if (items.isEmpty()) return null;
        AsosDailyItemDto item = items.get(0);
        if (item == null) return null;
        // 최저·최고 중 하나라도 없으면 반쪽짜리 기록을 남기지 않는다.
        Integer min = KmaValueParser.rounded(item.minTa());
        if (min == null) return null;
        Integer max = KmaValueParser.rounded(item.maxTa());
        if (max == null) return null;
        return new DailyWeather(date, AsosConditionMapper.of(KmaValueParser.decimal(item.avgTca()),
            KmaValueParser.decimal(item.sumRn())), min, max);
    }

    private record AsosDailyItemDto(@JsonProperty("minTa") String minTa, @JsonProperty("maxTa") String maxTa,
                                    @JsonProperty("avgTca") String avgTca, @JsonProperty("sumRn") String sumRn) {}
}
