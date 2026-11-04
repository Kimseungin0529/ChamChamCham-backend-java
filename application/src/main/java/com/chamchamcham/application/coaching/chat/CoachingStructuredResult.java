package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagSourceType;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingStructuredResult(
    @NonNull String summary,
    @NonNull CoachingRiskLevel riskLevel,
     double confidence,
    @NonNull List<CoachingObservation> observations,
    @NonNull String diagnosis,
    @NonNull List<CoachingRecommendation> recommendations,
    @NonNull List<CoachingNextAction> nextActions,
    @NonNull List<String> followUpQuestions,
    @NonNull List<CoachingCitationRef> citations,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = RecordQualityDefault.class) CoachingRecordQuality recordQuality,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LimitationsDefault.class) List<String> limitations
) {
    public CoachingStructuredResult {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(riskLevel, "riskLevel");
        Objects.requireNonNull(observations, "observations");
        Objects.requireNonNull(diagnosis, "diagnosis");
        Objects.requireNonNull(recommendations, "recommendations");
        Objects.requireNonNull(nextActions, "nextActions");
        Objects.requireNonNull(followUpQuestions, "followUpQuestions");
        Objects.requireNonNull(citations, "citations");
        Objects.requireNonNull(recordQuality, "recordQuality");
        Objects.requireNonNull(limitations, "limitations");
    }
    @NonNull public String getSummary() { return summary; }
    @NonNull public CoachingRiskLevel getRiskLevel() { return riskLevel; }
    public double getConfidence() { return confidence; }
    @NonNull public List<CoachingObservation> getObservations() { return observations; }
    @NonNull public String getDiagnosis() { return diagnosis; }
    @NonNull public List<CoachingRecommendation> getRecommendations() { return recommendations; }
    @NonNull public List<CoachingNextAction> getNextActions() { return nextActions; }
    @NonNull public List<String> getFollowUpQuestions() { return followUpQuestions; }
    @NonNull public List<CoachingCitationRef> getCitations() { return citations; }
    @NonNull public CoachingRecordQuality getRecordQuality() { return recordQuality; }
    @NonNull public List<String> getLimitations() { return limitations; }
    public static CoachingStructuredResult insufficientEvidence(String message) {
        return new CoachingStructuredResult(message, CoachingRiskLevel.UNKNOWN, 0.0, Collections.emptyList(), message, Collections.emptyList(), Collections.emptyList(), Collections.singletonList("최근 영농일지나 작물 상태 정보를 추가로 입력해주세요."), Collections.emptyList(), new CoachingRecordQuality(CoachingRecordQualityScore.UNKNOWN, Collections.emptyList(), "현재 근거만으로 기록 품질을 평가하기 어렵습니다."), Collections.singletonList("근거가 부족해 보수적으로 판단했습니다."));
    }
    public static final class RecordQualityDefault extends com.fasterxml.jackson.databind.JsonDeserializer<CoachingRecordQuality> {
        @Override public CoachingRecordQuality deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, CoachingRecordQuality.class);
        }
        @Override public CoachingRecordQuality getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new CoachingRecordQuality(); }
    }
    public static final class LimitationsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<String>> {
        @Override public List<String> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
        }
        @Override public List<String> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
}
