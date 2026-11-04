package com.chamchamcham.application.voice;

import com.chamchamcham.application.farming.FarmingRecordCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VoiceRecordCandidateAnalyzer {
    private VoiceRecordCandidateAnalyzer() {}

    public static List<String> missingFields(VoiceRecordCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        var missing = new ArrayList<String>();
        if (candidate.getFarmId() == null) missing.add("farmId");
        if (candidate.getCropId() == null) missing.add("cropId");
        if (candidate.getWorkType() == null) missing.add("workType");
        if (candidate.getWorkedAt() == null) missing.add("workedAt");
        var workType = candidate.getWorkType();
        if (workType != null && workType.getDetailRequired()) {
            var detail = switch (workType) {
                case FERTILIZING -> candidate.getFertilizing();
                case PEST_CONTROL -> candidate.getPestControl();
                case HARVEST -> candidate.getHarvest();
                case PLANTING -> plantingCompletionMarker(candidate.getPlanting());
                case WATERING, WEEDING, PRUNING, ETC -> Boolean.TRUE;
            };
            if (detail == null) missing.add("detail");
        }
        return missing;
    }

    private static Object plantingCompletionMarker(FarmingRecordCommand.PlantingDetail planting) {
        if (planting == null) return null;
        return switch (planting.getPlantingMethod()) {
            case SEED -> planting.getSeedAmount();
            case SEEDLING -> planting.getSeedlingCount();
        };
    }
}
