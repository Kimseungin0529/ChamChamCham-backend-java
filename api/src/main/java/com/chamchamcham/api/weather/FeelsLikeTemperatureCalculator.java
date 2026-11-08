package com.chamchamcham.api.weather;

import static java.lang.Math.*;

/**
 * 기상청이 공개한 체감온도 공식으로 계산하는 순수 함수 객체.
 * 외부 API가 아닌, 이미 조회된 기온/습도/풍속으로부터 계산한다.
 * - 여름철(5~9월): 열지수 기반 공식(습구온도 Tw를 거쳐 계산).
 * - 겨울철(10~4월): 풍속냉각 공식.
 * 두 구간은 12개월을 정확히 양분하며, 그 외의 "제3의" 구간은 없다.
 *
 * ⚠️ **아래 계수가 기상청 공식과 같은지는 검증되지 않았다(2026-07-16).**
 * 기상청 체감온도 API로 대조하려 했으나 접근할 수 없었다:
 * - `LivingWthrIdxServiceV5`(우리가 활용신청한 생활기상지수 3.0)에는 체감온도가 **없다**
 *   (`getSenTaIdxV5`/`getSenTaIdx` 모두 `API not found`. V5에 있는 건 자외선·대기확산뿐)
 * - `LivingWthrIdxServiceV4/getSenTaIdxV4`는 존재하지만 `Forbidden` — 별도 활용신청 필요
 * - 3.0 활용가이드 문서에도 공식이 실려 있지 않다
 *
 * 검증하려면 공공데이터포털에서 생활기상지수 구버전(V4) 활용신청을 하고 `getSenTaIdxV4`와
 * 대조해야 한다. 그때까지 이 값은 "그럴듯하지만 근거 미확인"이다.
 *
 * 다만 [SUMMER_MONTHS] 경계는 확인됐다 — 3.0 가이드의 체감온도(여름철) 항목에
 * "자료제공 기간 : 5월 1일 ~ 9월 30일"이라고 적혀 있다.
 */
public final class FeelsLikeTemperatureCalculator {
    public static final FeelsLikeTemperatureCalculator INSTANCE = new FeelsLikeTemperatureCalculator();
    private FeelsLikeTemperatureCalculator() {}
    public static Integer of(int temperature,Integer humidity,Double windSpeedMps,int month) {
        double ta=temperature;
        if (month>=5 && month<=9) return summer(ta,humidity);
        return winter(ta,windSpeedMps);
    }
    private static Integer winter(double ta,Double windSpeedMps) {
        if (windSpeedMps==null) return null;
        if (!(ta<=10.0 && windSpeedMps>=1.3)) return KmaValueParser.roundToInt(ta);
        double v=windSpeedMps*3.6;
        double feelsLike=13.12+0.6215*ta-11.37*pow(v,0.16)+0.3965*pow(v,0.16)*ta;
        return KmaValueParser.roundToInt(feelsLike);
    }
    private static Integer summer(double ta,Integer humidity) {
        if (humidity==null) return null;
        double rh=humidity;
        double tw=ta*atan(0.151977*sqrt(rh+8.313659))+atan(ta+rh)-atan(rh-1.676331)+0.00391838*pow(rh,1.5)*atan(0.023101*rh)-4.686035;
        double feelsLike=-0.2442+0.55399*tw+0.45535*ta-0.0022*pow(tw,2)+0.00278*tw*ta+3.0;
        return KmaValueParser.roundToInt(feelsLike);
    }
}
