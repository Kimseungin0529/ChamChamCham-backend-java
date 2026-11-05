package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackContent(
    @NonNull String summary,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ComparisonsDefault.class) List<ReportFeedbackContentItem> comparisons,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = StrengthsDefault.class) List<ReportFeedbackContentItem> strengths,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ImprovementsDefault.class) List<ReportFeedbackContentItem> improvements,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = NextActionsDefault.class) List<ReportFeedbackContentItem> nextActions
) {
    public ReportFeedbackContent {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(comparisons, "comparisons");
        Objects.requireNonNull(strengths, "strengths");
        Objects.requireNonNull(improvements, "improvements");
        Objects.requireNonNull(nextActions, "nextActions");
    }
    @NonNull public String getSummary() { return summary; }
    @NonNull public List<ReportFeedbackContentItem> getComparisons() { return comparisons; }
    @NonNull public List<ReportFeedbackContentItem> getStrengths() { return strengths; }
    @NonNull public List<ReportFeedbackContentItem> getImprovements() { return improvements; }
    @NonNull public List<ReportFeedbackContentItem> getNextActions() { return nextActions; }
    public static final class ComparisonsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<ReportFeedbackContentItem>> {
        @Override public List<ReportFeedbackContentItem> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<ReportFeedbackContentItem>>() {}));
        }
        @Override public List<ReportFeedbackContentItem> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
    public static final class StrengthsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<ReportFeedbackContentItem>> {
        @Override public List<ReportFeedbackContentItem> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<ReportFeedbackContentItem>>() {}));
        }
        @Override public List<ReportFeedbackContentItem> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
    public static final class ImprovementsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<ReportFeedbackContentItem>> {
        @Override public List<ReportFeedbackContentItem> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<ReportFeedbackContentItem>>() {}));
        }
        @Override public List<ReportFeedbackContentItem> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
    public static final class NextActionsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<ReportFeedbackContentItem>> {
        @Override public List<ReportFeedbackContentItem> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<ReportFeedbackContentItem>>() {}));
        }
        @Override public List<ReportFeedbackContentItem> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
    public ReportFeedbackContent normalizedParagraphs() {
        return new ReportFeedbackContent(normalizeParagraph(summary), normalized(comparisons), normalized(strengths), normalized(improvements), normalized(nextActions));
    }
    private static List<ReportFeedbackContentItem> normalized(List<ReportFeedbackContentItem> values) {
        var result = new ArrayList<ReportFeedbackContentItem>();
        for (var item : values) result.add(item.normalizedParagraph());
        return result;
    }
    public List<ReportFeedbackStructuredItem> items() {
        var result = new ArrayList<ReportFeedbackStructuredItem>();
        comparisons.forEach(it -> result.add(new ReportFeedbackStructuredItem(ReportFeedbackItemSection.COMPARISON, it)));
        strengths.forEach(it -> result.add(new ReportFeedbackStructuredItem(ReportFeedbackItemSection.STRENGTH, it)));
        improvements.forEach(it -> result.add(new ReportFeedbackStructuredItem(ReportFeedbackItemSection.IMPROVEMENT, it)));
        nextActions.forEach(it -> result.add(new ReportFeedbackStructuredItem(ReportFeedbackItemSection.NEXT_ACTION, it)));
        return Collections.unmodifiableList(result);
    }
    private static final java.util.regex.Pattern PARAGRAPH_BREAKS = java.util.regex.Pattern.compile("\\s*[\\r\\n]+\\s*");
    static String normalizeParagraph(String text) {
        text = PARAGRAPH_BREAKS.matcher(text).replaceAll(" ");
        int start = 0, end = text.length();
        while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++;
        while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--;
        return text.substring(start, end);
    }
}
