package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;

@Component
public class ReportFeedbackRetrievalQueryPlanner {
    public List<String> plan(ReportFeedbackContext context) {
        Objects.requireNonNull(context, "context");
        String crop = trim(context.getReport().getCropName()), work = context.getWorkType().getLabel();
        var statistics = context.getReport().getStatistics();
        var queries = new ArrayList<String>(); queries.add(crop + " " + work + " 재배 관리");
        int start = context.getReport().getStartsAt().getMonthValue(), end = context.getReport().getEndsAt().getMonthValue();
        queries.add(crop + " " + (start == end ? start + "월" : start + "월~" + end + "월") + " " + work + " 시기");
        if (statistics.get("averageIntervalDays") instanceof Number) queries.add(crop + " " + work + " 간격");
        switch (context.getWorkType()) {
            case PLANTING -> topValues(statistics, "propagationMethods", "code").stream().map(this::plantingQueryText).filter(Objects::nonNull).forEach(it -> queries.add(crop + " " + it + " 방법"));
            case WATERING -> topValues(statistics, "methodDistribution", "code").stream().map(this::irrigationQueryText).filter(Objects::nonNull).forEach(it -> queries.add(crop + " " + it + " 방법"));
            case FERTILIZING -> { topValues(statistics, "materialDistribution", "label").forEach(it -> queries.add(crop + " " + it + " 시비")); topValues(statistics, "methodDistribution", "code").stream().map(this::fertilizingQueryText).filter(Objects::nonNull).forEach(it -> queries.add(crop + " " + it + " 방법")); }
            case PEST_CONTROL -> { int count = 0; for (var row : listOfMaps(statistics, "targets")) { var value = row.get("target"); if (value != null && !blank(value.toString())) { queries.add(crop + " " + value + " 방제"); if (++count == 2) break; } } }
            case WEEDING -> topValues(statistics, "methodDistribution", "code").stream().map(this::weedingQueryText).filter(Objects::nonNull).forEach(it -> queries.add(crop + " " + it));
            case HARVEST -> topValues(statistics, "medicinalParts", "code").stream().map(this::cropPartQueryText).filter(Objects::nonNull).forEach(it -> queries.add(crop + " " + it + " 수확 시기"));
            case PRUNING, ETC -> { }
        }
        var unique = new ArrayList<>(new LinkedHashSet<>(queries));
        return new ArrayList<>(unique.subList(0, Math.min(5, unique.size())));
    }
    private List<String> topValues(Map<String, Object> statistics, String key, String field) {
        var entries = listOfMaps(statistics, key); entries.sort(Comparator.comparingInt(this::count).reversed());
        var result = new ArrayList<String>();
        for (var row : entries.subList(0, Math.min(2, entries.size()))) { var value = row.get(field); if (value != null && !blank(value.toString())) result.add(value.toString()); }
        return result;
    }
    private int count(Map<String, Object> row) { var value = row.get("count"); if (!(value instanceof Number)) value = row.get("recordCount"); return value instanceof Number number ? number.intValue() : 0; }
    private List<Map<String, Object>> listOfMaps(Map<String, Object> map, String key) {
        var result = new ArrayList<Map<String, Object>>();
        if (map.get(key) instanceof List<?> list) for (var item : list) if (item instanceof Map<?, ?> source) { var row = new LinkedHashMap<String, Object>(); source.forEach((name, value) -> { if (name instanceof String string) row.put(string, value); }); result.add(row); }
        return result;
    }
    private String plantingQueryText(String code) { return switch (code) { case "SEED" -> "파종"; case "SEEDLING" -> "정식"; case "PURCHASED" -> "모종 정식"; case "CUTTING" -> "삽목"; case "GRAFTING" -> "접목"; case "LAYERING" -> "휘묻이"; case "DIVISION" -> "분주"; case "TISSUE_CULTURE" -> "조직배양 묘 정식"; default -> null; }; }
    private String irrigationQueryText(String code) { return switch (code) { case "DRIP" -> "점적 관수"; case "SPRAYING" -> "살수 관수"; default -> null; }; }
    private String fertilizingQueryText(String code) { return switch (code) { case "SOIL" -> "토양 시비"; case "FOLIAR" -> "엽면 시비"; default -> null; }; }
    private String weedingQueryText(String code) { return switch (code) { case "HAND" -> "제초 관리"; case "MACHINE" -> "기계 제초"; case "MULCHING" -> "멀칭 제초"; case "HERBICIDE" -> "제초제 사용"; default -> null; }; }
    private String cropPartQueryText(String code) { return switch (code) { case "WHOLE_HERB" -> "전초"; case "ROOT_BARK" -> "뿌리"; case "RHIZOME" -> "근경"; case "LEAF" -> "잎"; case "FLOWER" -> "꽃"; case "FRUIT" -> "열매"; case "SEED" -> "종자"; case "STEM_BRANCH" -> "줄기"; default -> null; }; }
    private static boolean blank(String text) { return text.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)); }
    private static String trim(String text) { int start = 0, end = text.length(); while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++; while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--; return text.substring(start, end); }
}
