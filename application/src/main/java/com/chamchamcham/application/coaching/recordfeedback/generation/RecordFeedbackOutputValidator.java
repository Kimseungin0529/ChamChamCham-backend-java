package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import java.util.*;

public final class RecordFeedbackOutputValidator {
    private RecordFeedbackOutputValidator() {}
    public static List<String> validate(RecordFeedbackContent content, RecordFeedbackContext context, List<RecordFeedbackEvidence> evidence) {
        Objects.requireNonNull(content); Objects.requireNonNull(context); Objects.requireNonNull(evidence);
        var weatherIds = new LinkedHashSet<String>();
        if (context.getWeather() != null) { weatherIds.add("weather:current"); context.getWeather().getForecastDays().forEach(it -> weatherIds.add("weather:" + it.getDate())); }
        var documentIds = new LinkedHashSet<String>();
        for (var it : evidence) if (!blank(it.getId())) documentIds.add(it.getId());
        var ids = new LinkedHashSet<String>(); ids.add(context.recordCitationId()); ids.addAll(weatherIds); ids.addAll(documentIds);
        var warnings = new ArrayList<String>();
        validateItem("good_point", content.getGoodPoint(), ids, warnings);
        if (content.getNextActions().size() < 2 || content.getNextActions().size() > 3) warnings.add("action_count");
        for (int i = 0; i < content.getNextActions().size(); i++) {
            var action = content.getNextActions().get(i);
            validateItem("next_action_" + i, new RecordFeedbackGoodPoint(action.getBasis(), action.getText(), action.getEvidenceRefs()), ids, warnings);
            if (action.getCategory() == RecordFeedbackActionCategory.WEATHER && action.getEvidenceRefs().stream().noneMatch(weatherIds::contains)) warnings.add("weather_action_without_weather_evidence");
            if (action.getCategory() == RecordFeedbackActionCategory.PEST_DISEASE && action.getEvidenceRefs().stream().noneMatch(documentIds::contains)) warnings.add("pest_disease_action_without_document_evidence");
        }
        var result = new ArrayList<>(new LinkedHashSet<>(warnings));
        return switch (result.size()) { case 0 -> Collections.emptyList(); case 1 -> Collections.singletonList(result.getFirst()); default -> result; };
    }
    private static void validateItem(String prefix, RecordFeedbackGoodPoint item, Set<String> ids, List<String> warnings) {
        if (blank(item.getBasis())) warnings.add(prefix + "_basis_blank");
        if (blank(item.getText())) warnings.add(prefix + "_text_blank");
        if (item.getEvidenceRefs().stream().noneMatch(it -> !blank(it))) warnings.add(prefix + "_evidence_refs_blank");
        if (item.getEvidenceRefs().stream().anyMatch(RecordFeedbackOutputValidator::blank)) warnings.add(prefix + "_evidence_ref_blank");
        if (item.getText().length() < 15 || item.getText().length() > 60) warnings.add(prefix + "_text_length");
        var text = item.getText(); int end = text.length();
        while (end > 0 && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--;
        while (end > 0 && ".!?…。".indexOf(text.charAt(end - 1)) >= 0) end--;
        if (!text.substring(0, end).endsWith("요")) warnings.add(prefix + "_text_tone");
        if (CoachingTextPolicy.containsEnglishLetter(item.getText())) warnings.add(prefix + "_text_english");
        for (String ref : item.getEvidenceRefs()) if (!blank(ref) && !ids.contains(ref)) warnings.add("unknown_evidence");
    }
    private static boolean blank(String text) { return text.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)); }
}
