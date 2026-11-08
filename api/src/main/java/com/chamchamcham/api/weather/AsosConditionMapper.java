package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.WeatherCondition;

/** ASOS의 전운량과 일강수량으로 근사한다. 빈 일강수량(null)은 무강수로 취급한다. */
public final class AsosConditionMapper {
    public static final AsosConditionMapper INSTANCE = new AsosConditionMapper();
    private AsosConditionMapper() {}
    public static WeatherCondition of(Double avgTca, Double sumRn) {
        if (sumRn != null && sumRn > 0.0) return WeatherCondition.RAIN;
        if (avgTca == null) return WeatherCondition.UNKNOWN;
        if (avgTca <= 2.0) return WeatherCondition.CLEAR;
        if (avgTca <= 7.0) return WeatherCondition.PARTLY_CLOUDY;
        return WeatherCondition.CLOUDY;
    }
}
