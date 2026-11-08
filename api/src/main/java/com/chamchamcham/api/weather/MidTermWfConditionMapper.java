package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.WeatherCondition;

/** 비/눈 등 강수 표현을 먼저 검사하며, 흐림도 '흐'로 매칭한다. */
public final class MidTermWfConditionMapper {
    public static final MidTermWfConditionMapper INSTANCE = new MidTermWfConditionMapper();
    private MidTermWfConditionMapper() {}
    public static WeatherCondition of(String wf) {
        if (wf == null || wf.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) return WeatherCondition.UNKNOWN;
        if (wf.contains("비/눈")) return WeatherCondition.RAIN_SNOW;
        if (wf.contains("소나기")) return WeatherCondition.SHOWER;
        if (wf.contains("비")) return WeatherCondition.RAIN;
        if (wf.contains("눈")) return WeatherCondition.SNOW;
        if (wf.contains("흐")) return WeatherCondition.CLOUDY;
        if (wf.contains("구름많")) return WeatherCondition.PARTLY_CLOUDY;
        if (wf.contains("맑")) return WeatherCondition.CLEAR;
        return WeatherCondition.UNKNOWN;
    }
}
