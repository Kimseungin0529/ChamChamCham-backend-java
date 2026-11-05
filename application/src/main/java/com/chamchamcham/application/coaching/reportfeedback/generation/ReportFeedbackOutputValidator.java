package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import java.util.*;

public final class ReportFeedbackOutputValidator {
    private ReportFeedbackOutputValidator() {}
    public static List<String> validate(ReportFeedbackContent content, ReportFeedbackContext context, List<ReportFeedbackEvidence> documents) {
        Objects.requireNonNull(content); Objects.requireNonNull(context); Objects.requireNonNull(documents);
        var warnings = new ArrayList<String>();
        if (blank(content.getSummary())) warnings.add("summary_blank");
        else if (content.getSummary().length() < 20 || content.getSummary().length() > 65) warnings.add("summary_text_length");
        if (context.getComparisons().isEmpty()) { if (!content.getComparisons().isEmpty()) warnings.add("comparison_not_available"); }
        else if (content.getComparisons().size() != 1) warnings.add("comparison_count");
        if (content.getStrengths().size() != 1) warnings.add("strength_count");
        if (content.getImprovements().size() != 1) warnings.add("improvement_count");
        if (content.getNextActions().size() != 1) warnings.add("next_action_count");
        String currentRef = "report:" + context.getReport().getId();
        String previousRef = context.getPreviousReport() == null ? null : "report:" + context.getPreviousReport().getId();
        var allowed = new HashSet<String>(); allowed.add(currentRef); context.getRecords().forEach(it -> allowed.add("record:" + it.getId())); if (previousRef != null) allowed.add(previousRef); documents.forEach(it -> allowed.add(it.getId()));
        for (var structured : content.items()) {
            var item = structured.getItem(); String prefix = structured.getSection().name().toLowerCase(Locale.ROOT);
            if (blank(item.getBasis())) warnings.add(prefix + "_basis_blank");
            if (blank(item.getText())) warnings.add(prefix + "_text_blank");
            else if (item.getText().length() < 20 || item.getText().length() > 65) warnings.add(prefix + "_text_length");
            if (item.getEvidenceRefs().stream().noneMatch(it -> !blank(it))) warnings.add(prefix + "_evidence_refs_blank");
            item.getEvidenceRefs().stream().filter(it -> !blank(it) && !allowed.contains(it)).forEach(it -> warnings.add("unknown_evidence:" + it));
            if (structured.getSection() == ReportFeedbackItemSection.COMPARISON && !context.getComparisons().isEmpty()) {
                if (!item.getEvidenceRefs().contains(currentRef)) warnings.add("comparison_current_report_ref_required");
                if (previousRef == null || !item.getEvidenceRefs().contains(previousRef)) warnings.add("comparison_previous_report_ref_required");
            }
        }
        var result = new ArrayList<>(new LinkedHashSet<>(warnings)); return switch (result.size()) { case 0 -> Collections.emptyList(); case 1 -> Collections.singletonList(result.getFirst()); default -> result; };
    }
    private static boolean blank(String text) { return text.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)); }
}
