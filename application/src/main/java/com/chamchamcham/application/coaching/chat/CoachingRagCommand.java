package com.chamchamcham.application.coaching.chat;

import java.time.LocalDate;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingRagCommand(
    @NonNull UUID memberId,
    @NonNull String question,
    @Nullable UUID farmId,
    @Nullable UUID cropId,
    @Nullable UUID recordId,
    @Nullable LocalDate periodStart,
    @Nullable LocalDate periodEnd,
    @Nullable Integer topK
) {
    public CoachingRagCommand {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(question, "question");
    }
    @NonNull public UUID getMemberId() { return memberId; }
    @NonNull public String getQuestion() { return question; }
    @Nullable public UUID getFarmId() { return farmId; }
    @Nullable public UUID getCropId() { return cropId; }
    @Nullable public UUID getRecordId() { return recordId; }
    @Nullable public LocalDate getPeriodStart() { return periodStart; }
    @Nullable public LocalDate getPeriodEnd() { return periodEnd; }
    @Nullable public Integer getTopK() { return topK; }
}
