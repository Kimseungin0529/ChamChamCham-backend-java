package com.chamchamcham.application.weather;

import org.springframework.lang.NonNull;

public enum WeatherCondition {
    /** SKY=1. getVilageFcst */
    CLEAR("맑음"),

    /** SKY=3. getVilageFcst */
    PARTLY_CLOUDY("구름많음"),

    /** SKY=4. getVilageFcst */
    CLOUDY("흐림"),

    /** PTY=1. 실황(getUltraSrtNcst) · getVilageFcst */
    RAIN("비"),

    /** PTY=2. 실황(getUltraSrtNcst) · getVilageFcst */
    RAIN_SNOW("비/눈"),

    /** PTY=3. 실황(getUltraSrtNcst) · getVilageFcst */
    SNOW("눈"),

    /** PTY=4. getVilageFcst 전용 */
    SHOWER("소나기"),

    /** PTY=5. 실황(getUltraSrtNcst) 전용 */
    DRIZZLE("빗방울"),

    /** PTY=6. 실황(getUltraSrtNcst) 전용 */
    DRIZZLE_SNOW("빗방울눈날림"),

    /** PTY=7. 실황(getUltraSrtNcst) 전용 */
    SNOW_FLURRY("눈날림"),

    /** 매핑 불가 */
    UNKNOWN("정보없음");

    private final String text;

    WeatherCondition(String text) { this.text = text; }

    @NonNull
    public String getText() { return text; }
}
