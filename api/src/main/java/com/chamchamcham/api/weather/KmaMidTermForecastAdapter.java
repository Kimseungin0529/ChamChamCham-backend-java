package com.chamchamcham.api.weather;

import java.util.Objects;
import com.chamchamcham.application.weather.DailyForecast;
import com.chamchamcham.application.weather.MidTermForecastPort;
import com.chamchamcham.application.weather.WeatherLocation;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KmaMidTermForecastAdapter implements MidTermForecastPort {
    private static final DateTimeFormatter TM_FC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final KmaApiClient kmaApiClient;
    private final KmaBaseTime kmaBaseTime;
    private final KmaProperties properties;

    public KmaMidTermForecastAdapter(KmaApiClient kmaApiClient, KmaBaseTime kmaBaseTime, KmaProperties properties) {
        this.kmaApiClient = Objects.requireNonNull(kmaApiClient, "kmaApiClient");
        this.kmaBaseTime = Objects.requireNonNull(kmaBaseTime, "kmaBaseTime");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public DailyForecast fetch(WeatherLocation location, LocalDate date) {
        MidFcstStation station = NearestMidFcstStationResolver.resolve(location.getLatitude(), location.getLongitude());
        String tmFc = kmaBaseTime.resolveMidFcst();
        LocalDate tmFcDate = LocalDate.parse(tmFc.substring(0, 8), TM_FC_DATE_FORMAT);
        int n = (int) ChronoUnit.DAYS.between(tmFcDate, date);
        if (n < 3 || n > 10) return null;
        List<JsonNode> taItems = items("getMidTa", station.getTaRegId(), tmFc);
        String landRegId = MidTermLandRegion.of(station.getTaRegId());
        List<JsonNode> landItems = landRegId == null ? List.of() : items("getMidLandFcst", landRegId, tmFc);
        JsonNode taNode = taItems.isEmpty() ? null : taItems.get(0);
        JsonNode landNode = landItems.isEmpty() ? null : landItems.get(0);
        Integer min = KmaValueParser.integer(text(taNode, "taMin" + n));
        Integer max = KmaValueParser.integer(text(taNode, "taMax" + n));
        // 주간예보는 낮 날씨 우선. 8~10일에는 Am/Pm 없는 필드로 내려온다.
        String wf = text(landNode, "wf" + n + "Pm");
        if (wf == null) wf = text(landNode, "wf" + n + "Am");
        if (wf == null) wf = text(landNode, "wf" + n);
        if (min == null && max == null && wf == null) return null;
        return new DailyForecast(date, MidTermWfConditionMapper.of(wf), min, max);
    }

    private List<JsonNode> items(String operation, String regId, String tmFc) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("regId", regId);
        params.put("tmFc", tmFc);
        params.put("numOfRows", "10");
        return kmaApiClient.getItems(properties.getBaseUrl().getMidFcst(), operation, params, JsonNode.class);
    }

    private String text(JsonNode node, String key) {
        return node == null || node.get(key) == null ? null : node.get(key).asText();
    }
}
