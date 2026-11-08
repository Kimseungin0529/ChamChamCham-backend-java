package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.WeatherCondition;

/** 실황과 단기예보의 PTY 합집합을 SKY보다 먼저 판정한다. */
public final class SkyPtyConditionMapper {
    public static final SkyPtyConditionMapper INSTANCE = new SkyPtyConditionMapper();
    private SkyPtyConditionMapper() {}
    public static WeatherCondition of(String sky, String pty) {
        if (pty != null) {
            WeatherCondition precipitation = switch (pty) {
                case "1" -> WeatherCondition.RAIN;
                case "2" -> WeatherCondition.RAIN_SNOW;
                case "3" -> WeatherCondition.SNOW;
                case "4" -> WeatherCondition.SHOWER;
                case "5" -> WeatherCondition.DRIZZLE;
                case "6" -> WeatherCondition.DRIZZLE_SNOW;
                case "7" -> WeatherCondition.SNOW_FLURRY;
                default -> null;
            };
            if (precipitation != null) return precipitation;
        }
        if (sky == null) return WeatherCondition.UNKNOWN;
        return switch (sky) {
            case "1" -> WeatherCondition.CLEAR;
            case "3" -> WeatherCondition.PARTLY_CLOUDY;
            case "4" -> WeatherCondition.CLOUDY;
            default -> WeatherCondition.UNKNOWN;
        };
    }
}
