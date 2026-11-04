package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class CoachingStructuredOutputValidator {
    public RagAuditResult validate(CoachingStructuredResult result, Set<String> allowedCitationIds) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(allowedCitationIds, "allowedCitationIds");
        var warnings = new ArrayList<String>();
        if (!(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0)) warnings.add("invalid_confidence");
        var citationIds = new ArrayList<String>();
        result.getObservations().forEach(it -> citationIds.addAll(it.getCitationIds()));
        result.getRecommendations().forEach(it -> citationIds.addAll(it.getCitationIds()));
        result.getNextActions().forEach(it -> citationIds.addAll(it.getCitationIds()));
        result.getCitations().forEach(it -> citationIds.add(it.getChunkId()));
        result.getRecommendations().stream().filter(it -> it.getCitationIds().isEmpty()).forEach(it -> warnings.add("recommendation_without_citation:" + it.getAction()));
        result.getNextActions().stream().filter(it -> it.getCitationIds().isEmpty()).forEach(it -> warnings.add("next_action_without_citation:" + it.getAction()));
        for (String id : new LinkedHashSet<>(citationIds)) if (!allowedCitationIds.contains(id)) warnings.add("unknown_citation:" + id);
        boolean failure = warnings.stream().anyMatch(it -> it.equals("invalid_confidence") || it.startsWith("unknown_citation:") || it.startsWith("recommendation_without_citation:") || it.startsWith("next_action_without_citation:"));
        return new RagAuditResult(failure ? RagAuditStatus.FAIL : warnings.isEmpty() ? RagAuditStatus.PASS : RagAuditStatus.WARN, distinct(warnings), distinct(citationIds));
    }
    private static List<String> distinct(List<String> values) {
        var result = new ArrayList<>(new LinkedHashSet<>(values));
        return switch (result.size()) { case 0 -> Collections.emptyList(); case 1 -> Collections.singletonList(result.getFirst()); default -> result; };
    }
}
