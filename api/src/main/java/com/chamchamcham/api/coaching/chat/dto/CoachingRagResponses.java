package com.chamchamcham.api.coaching.chat.dto;


import com.chamchamcham.application.coaching.chat.*;
import com.chamchamcham.application.coaching.common.RagAuditStatus;
import com.chamchamcham.application.coaching.common.RagSourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;

public final class CoachingRagResponses {
    private CoachingRagResponses() { }

    public record QueryResponse(
            StructuredResultResponse result,
            AuditResponse audit,
            ModelResponse model) {
        public QueryResponse {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(audit, "audit");
            Objects.requireNonNull(model, "model");
        }
        public StructuredResultResponse getResult() { return result; }
        public AuditResponse getAudit() { return audit; }
        public ModelResponse getModel() { return model; }

        public static QueryResponse from(CoachingRagResult result) {
            return new QueryResponse(StructuredResultResponse.from(result.getResult()),
                    new AuditResponse(result.getAudit().getStatus(), result.getAudit().getWarnings()),
                    new ModelResponse(result.getModel().getEmbedding(), result.getModel().getChat()));
        }
    }

    public record StructuredResultResponse(
            String summary,
            CoachingRiskLevel riskLevel,
            double confidence,
            List<ObservationResponse> observations,
            String diagnosis,
            List<RecommendationResponse> recommendations,
            List<NextActionResponse> nextActions,
            List<String> followUpQuestions,
            List<CitationResponse> citations,
            RecordQualityResponse recordQuality,
            List<String> limitations) {
        public StructuredResultResponse {
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
        public String getSummary() { return summary; }
        public CoachingRiskLevel getRiskLevel() { return riskLevel; }
        public double getConfidence() { return confidence; }
        public List<ObservationResponse> getObservations() { return observations; }
        public String getDiagnosis() { return diagnosis; }
        public List<RecommendationResponse> getRecommendations() { return recommendations; }
        public List<NextActionResponse> getNextActions() { return nextActions; }
        public List<String> getFollowUpQuestions() { return followUpQuestions; }
        public List<CitationResponse> getCitations() { return citations; }
        public RecordQualityResponse getRecordQuality() { return recordQuality; }
        public List<String> getLimitations() { return limitations; }

        public static StructuredResultResponse from(CoachingStructuredResult result) {
            var labels = new LinkedHashMap<String, String>();
            for (int index = 0; index < result.getCitations().size(); index++) {
                var citation = result.getCitations().get(index);
                labels.put(citation.getChunkId(), displayLabel(index) + ": " + citation.getLabel());
            }
            var observations = new ArrayList<ObservationResponse>(result.getObservations().size());
            for (var observation : result.getObservations()) {
                observations.add(new ObservationResponse(observation.getTitle(), observation.getDetail(), observation.getCitationIds(),
                        citationLabels(observation.getCitationIds(), labels)));
            }
            var recommendations = new ArrayList<RecommendationResponse>(result.getRecommendations().size());
            for (var recommendation : result.getRecommendations()) {
                recommendations.add(new RecommendationResponse(recommendation.getPriority(), recommendation.getAction(), recommendation.getReason(),
                        recommendation.getCaution(), recommendation.getCitationIds(), citationLabels(recommendation.getCitationIds(), labels)));
            }
            var nextActions = new ArrayList<NextActionResponse>(result.getNextActions().size());
            for (var action : result.getNextActions()) {
                nextActions.add(new NextActionResponse(action.getDue(), action.getAction(), action.getCitationIds(), citationLabels(action.getCitationIds(), labels)));
            }
            var citations = new ArrayList<CitationResponse>(result.getCitations().size());
            for (int index = 0; index < result.getCitations().size(); index++) {
                var citation = result.getCitations().get(index);
                citations.add(new CitationResponse(citation.getChunkId(), citation.getLabel(), citation.getSourceType(), displayLabel(index),
                        citation.getDocumentTitle(), citation.getPage(), citation.getSource()));
            }
            return new StructuredResultResponse(result.getSummary(), result.getRiskLevel(), result.getConfidence(), observations,
                    result.getDiagnosis(), recommendations, nextActions, result.getFollowUpQuestions(), citations,
                    RecordQualityResponse.from(result.getRecordQuality()), result.getLimitations());
        }

        private static String displayLabel(int index) { return "근거 " + (index + 1); }

        private static List<String> citationLabels(List<String> ids, Map<String, String> labels) {
            var result = new ArrayList<String>(ids.size());
            for (var id : ids) result.add(labels.getOrDefault(id, "근거 확인 필요"));
            return result;
        }
    }

    public record ObservationResponse(
            String title,
            String detail,
            List<String> citationIds,
            List<String> citationLabels) {
        public ObservationResponse {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(detail, "detail");
            Objects.requireNonNull(citationIds, "citationIds");
            Objects.requireNonNull(citationLabels, "citationLabels");
        }
        public String getTitle() { return title; }
        public String getDetail() { return detail; }
        public List<String> getCitationIds() { return citationIds; }
        public List<String> getCitationLabels() { return citationLabels; }
    }

    public record RecommendationResponse(
            CoachingPriority priority,
            String action,
            String reason,
            @Nullable String caution,
            List<String> citationIds,
            List<String> citationLabels) {
        public RecommendationResponse {
            Objects.requireNonNull(priority, "priority");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(citationIds, "citationIds");
            Objects.requireNonNull(citationLabels, "citationLabels");
        }
        public CoachingPriority getPriority() { return priority; }
        public String getAction() { return action; }
        public String getReason() { return reason; }
        public String getCaution() { return caution; }
        public List<String> getCitationIds() { return citationIds; }
        public List<String> getCitationLabels() { return citationLabels; }
    }

    public record NextActionResponse(
            CoachingActionDue due,
            String action,
            List<String> citationIds,
            List<String> citationLabels) {
        public NextActionResponse {
            Objects.requireNonNull(due, "due");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(citationIds, "citationIds");
            Objects.requireNonNull(citationLabels, "citationLabels");
        }
        public CoachingActionDue getDue() { return due; }
        public String getAction() { return action; }
        public List<String> getCitationIds() { return citationIds; }
        public List<String> getCitationLabels() { return citationLabels; }
    }

    public record CitationResponse(
            String chunkId,
            String label,
            RagSourceType sourceType,
            String displayLabel,
            @Nullable String documentTitle,
            @Nullable Integer page,
            @Nullable String source) {
        public CitationResponse {
            Objects.requireNonNull(chunkId, "chunkId");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(sourceType, "sourceType");
            Objects.requireNonNull(displayLabel, "displayLabel");
        }
        public String getChunkId() { return chunkId; }
        public String getLabel() { return label; }
        public RagSourceType getSourceType() { return sourceType; }
        public String getDisplayLabel() { return displayLabel; }
        public String getDocumentTitle() { return documentTitle; }
        public Integer getPage() { return page; }
        public String getSource() { return source; }
    }

    public record RecordQualityResponse(
            CoachingRecordQualityScore score,
            List<String> missingOrWeakFields,
            String comment) {
        public RecordQualityResponse {
            Objects.requireNonNull(score, "score");
            Objects.requireNonNull(missingOrWeakFields, "missingOrWeakFields");
            Objects.requireNonNull(comment, "comment");
        }
        public CoachingRecordQualityScore getScore() { return score; }
        public List<String> getMissingOrWeakFields() { return missingOrWeakFields; }
        public String getComment() { return comment; }

        public static RecordQualityResponse from(CoachingRecordQuality source) {
            return new RecordQualityResponse(source.getScore(), source.getMissingOrWeakFields(), source.getComment());
        }
    }

    public record AuditResponse(
            RagAuditStatus status,
            List<String> warnings) {
        public AuditResponse {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(warnings, "warnings");
        }
        public RagAuditStatus getStatus() { return status; }
        public List<String> getWarnings() { return warnings; }
    }

    public record ModelResponse(
            String embedding,
            String chat) {
        public ModelResponse {
            Objects.requireNonNull(embedding, "embedding");
            Objects.requireNonNull(chat, "chat");
        }
        public String getEmbedding() { return embedding; }
        public String getChat() { return chat; }
    }
}
