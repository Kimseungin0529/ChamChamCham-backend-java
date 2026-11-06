package com.chamchamcham.api.coaching.chat.dto;


import com.chamchamcham.application.coaching.chat.CoachingRagCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

public final class CoachingRagRequests {
    private CoachingRagRequests() { }

    public record QueryRequest(
            @NotBlank(message = "질문을 입력해주세요") String question,
            @Nullable UUID farmId,
            @Nullable UUID cropId,
            @Nullable UUID recordId,
            @Nullable LocalDate periodStart,
            @Nullable LocalDate periodEnd,
            @Nullable @Min(value = 1, message = "topK는 1 이상이어야 합니다")
            @Max(value = 20, message = "topK는 20 이하여야 합니다") Integer topK) {
        public QueryRequest { Objects.requireNonNull(question, "question"); }
        public String getQuestion() { return question; }
        public UUID getFarmId() { return farmId; }
        public UUID getCropId() { return cropId; }
        public UUID getRecordId() { return recordId; }
        public LocalDate getPeriodStart() { return periodStart; }
        public LocalDate getPeriodEnd() { return periodEnd; }
        public Integer getTopK() { return topK; }

        public CoachingRagCommand toCommand(UUID memberId) {
            return new CoachingRagCommand(memberId, question, farmId, cropId, recordId, periodStart, periodEnd, topK);
        }
    }
}
