package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.WorkType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

@Component
public class RecordFeedbackRetrievalQueryPlanner {
    public List<RecordFeedbackRetrievalQuery> plan(RecordFeedbackContext context) {
        Objects.requireNonNull(context);
        String crop = trim(context.getCrop().getName());
        var queries = new ArrayList<RecordFeedbackRetrievalQuery>();
        queries.add(new RecordFeedbackRetrievalQuery(crop + " " + context.getRecord().getWorkType().getLabel() + " 재배 관리 약용작물", "crop_work_type"));
        String memo = trim(context.getRecord().getMemo());
        if (!memo.isEmpty()) queries.add(new RecordFeedbackRetrievalQuery(crop + " " + memo.substring(0, Math.min(120, memo.length())), "memo_text"));
        var weather = context.getWeather();
        if (weather != null) {
            var rainfall = weather.getForecastDays().stream().map(RecordFeedbackForecastDay::getRainfallMm).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null);
            var maxTemp = weather.getForecastDays().stream().map(RecordFeedbackForecastDay::getMaxTemperatureC).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.valueOf(weather.getCurrent().getTemperatureC()));
            if (rainfall != null && rainfall.compareTo(BigDecimal.valueOf(30)) >= 0) queries.add(new RecordFeedbackRetrievalQuery(crop + " 강우 과습 배수 병해충", "rain_wet_weather"));
            else if (maxTemp.compareTo(BigDecimal.valueOf(30)) >= 0) queries.add(new RecordFeedbackRetrievalQuery(crop + " 고온 생육 관리", "hot_weather"));
            var forecast = forecastWeatherRiskQuery(crop, weather.getForecastDays()); if (forecast != null) queries.add(forecast);
        }
        if (context.getRecord().getWorkType() == WorkType.PEST_CONTROL && context.getRecord().getDetail() instanceof PestControlFeedbackDetail detail && detail.getPestName() != null && !trim(detail.getPestName()).isEmpty()) queries.add(new RecordFeedbackRetrievalQuery(crop + " " + trim(detail.getPestName()) + " 방제", "target_pest_or_disease"));
        if (context.getRecord().getWorkType() == WorkType.HARVEST) {
            var part = context.getRecord().getDetail() instanceof HarvestFeedbackDetail detail ? detail.getMedicinalPart() : null;
            queries.add(new RecordFeedbackRetrievalQuery("약용작물 " + (part == null ? "" : recordFeedbackLabel(part) + " ") + "수확 적기 " + crop, "harvest_use_part"));
        }
        var unique = new LinkedHashMap<String, RecordFeedbackRetrievalQuery>(); queries.forEach(it -> unique.putIfAbsent(it.getQuery(), it));
        var result = new ArrayList<>(unique.values());
        if (result.size() == 1) return Collections.singletonList(result.getFirst());
        return new ArrayList<>(result.subList(0, Math.min(6, result.size())));
    }
    private RecordFeedbackRetrievalQuery forecastWeatherRiskQuery(String crop, List<RecordFeedbackForecastDay> days) {
        if (days.isEmpty()) return null;
        Set<String> flags = new HashSet<>(); days.forEach(it -> it.getRiskFlags().forEach(flag -> flags.add(flag.toUpperCase(Locale.ROOT))));
        var rain = days.stream().map(RecordFeedbackForecastDay::getRainfallMm).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        var humidity = days.stream().map(RecordFeedbackForecastDay::getHumidityPct).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        var temperature = days.stream().map(RecordFeedbackForecastDay::getMaxTemperatureC).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        var wind = days.stream().map(RecordFeedbackForecastDay::getWindSpeedMs).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        var probability = days.stream().map(RecordFeedbackForecastDay::getRainProbabilityPct).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        if (!Collections.disjoint(flags, Set.of("HEAVY_RAIN", "RAIN", "HIGH_HUMIDITY", "WET", "TYPHOON")) || rain.compareTo(BigDecimal.valueOf(20)) >= 0 || probability >= 70 || humidity.compareTo(BigDecimal.valueOf(85)) >= 0) return new RecordFeedbackRetrievalQuery(crop + " 강우 예보 배수 과습 병해충", "forecast_rain_wet_weather");
        if (!Collections.disjoint(flags, Set.of("DRY", "DROUGHT"))) return new RecordFeedbackRetrievalQuery(crop + " 고온 건조 예보 관수 관리", "forecast_dry_weather");
        if (!Collections.disjoint(flags, Set.of("HOT", "HEAT", "HIGH_TEMPERATURE")) || temperature.compareTo(BigDecimal.valueOf(30)) >= 0) return new RecordFeedbackRetrievalQuery(crop + " 고온 예보 생육 관리", "forecast_hot_weather");
        if (!Collections.disjoint(flags, Set.of("STRONG_WIND", "WIND", "GALE")) || wind.compareTo(BigDecimal.valueOf(8)) >= 0) return new RecordFeedbackRetrievalQuery(crop + " 강풍 예보 방제 작업 안전", "forecast_wind_weather");
        return null;
    }
    private static String recordFeedbackLabel(CropUsePartCategory part) { return switch (part) { case WHOLE_HERB -> "전초"; case ROOT_BARK -> "뿌리·껍질"; case RHIZOME -> "뿌리줄기"; case LEAF -> "잎"; case FLOWER -> "꽃"; case FRUIT -> "열매/과실"; case SEED -> "종자"; case STEM_BRANCH -> "줄기/가지"; case UNKNOWN -> "기타"; }; }
    private static String trim(String text) { int start = 0, end = text.length(); while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++; while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--; return text.substring(start, end); }
}
