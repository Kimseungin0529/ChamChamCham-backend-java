package com.chamchamcham.api.weather;

import java.util.Objects;
import com.chamchamcham.application.weather.UvIndexPort;
import com.chamchamcham.application.weather.WeatherLocation;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KmaUvIndexAdapter implements UvIndexPort {
    private final KmaApiClient kmaApiClient;
    private final KmaBaseTime kmaBaseTime;
    private final KmaProperties properties;

    public KmaUvIndexAdapter(KmaApiClient kmaApiClient, KmaBaseTime kmaBaseTime, KmaProperties properties) {
        this.kmaApiClient = Objects.requireNonNull(kmaApiClient, "kmaApiClient");
        this.kmaBaseTime = Objects.requireNonNull(kmaBaseTime, "kmaBaseTime");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public Integer fetch(WeatherLocation location) {
        String pnu = location.getPnu();
        if (pnu == null || pnu.length() < 5) return null;
        // 법정동 전체가 아닌 시군구(앞 5자리)에 00000을 붙여야 한다.
        String areaNo = pnu.substring(0, 5) + "00000";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("areaNo", areaNo);
        params.put("time", kmaBaseTime.resolveUv());
        params.put("numOfRows", "10");
        List<JsonNode> items = kmaApiClient.getItems(properties.getBaseUrl().getUv(), "getUVIdxV5", params, JsonNode.class);
        JsonNode first = items.isEmpty() ? null : items.get(0);
        return first == null || first.get("h0") == null ? null : KmaValueParser.integer(first.get("h0").asText());
    }
}
