package com.chamchamcham.api.voice.dto;

import com.chamchamcham.api.farming.dto.FarmingRecordRequests;
import com.chamchamcham.application.voice.VoiceRecordCandidate;
import com.chamchamcham.application.voice.VoiceSessionCommand;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.voice.VoiceTurnRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class VoiceSessionRequests {
    private VoiceSessionRequests() {}

    public record SubmitTurnsRequest(
        @NotEmpty(message = "대화 턴이 비어있습니다")
        @Size(max = 50, message = "대화 턴은 최대 50개까지 제출할 수 있습니다")
        @NonNull List<TurnRequest> turns,
        @Valid
        @NonNull CandidateRequest candidate
    ) {
        public SubmitTurnsRequest {
            Objects.requireNonNull(turns, "turns");
            Objects.requireNonNull(candidate, "candidate");
        }
        @NonNull public List<TurnRequest> getTurns() { return turns; }
        @NonNull public CandidateRequest getCandidate() { return candidate; }
        public VoiceSessionCommand.SubmitTurns toCommand(UUID memberId, UUID sessionId) {
            return new VoiceSessionCommand.SubmitTurns(memberId,
                sessionId,
                turns.stream().map(t -> new VoiceSessionCommand.TurnInput(t.role(),
                t.content(),
                t.extractedFields())).collect(Collectors.toCollection(ArrayList::new)),
                candidate.toCandidate());
        }
    }

    public record TurnRequest(
        @NonNull VoiceTurnRole role,
        @NotBlank(message = "발화 내용을 입력해주세요")
        @Size(max = 4000, message = "발화 내용은 4000자 이내여야 합니다")
        @NonNull String content,
        @Nullable String extractedFields
    ) {
        public TurnRequest {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(content, "content");
        }
        @NonNull public VoiceTurnRole getRole() { return role; }
        @NonNull public String getContent() { return content; }
        @Nullable public String getExtractedFields() { return extractedFields; }
    }

    public record CandidateRequest(
        @Nullable UUID farmId,
        @Nullable UUID cropId,
        @Nullable WorkType workType,
        @Nullable LocalDateTime workedAt,
        @Nullable String weatherCondition,
        @Nullable Integer weatherTemperature,
        @Nullable String memo,
        @Valid
        @Nullable FarmingRecordRequests.PlantingDetailRequest planting,
        @Valid
        @Nullable FarmingRecordRequests.WateringDetailRequest watering,
        @Valid
        @Nullable FarmingRecordRequests.FertilizingDetailRequest fertilizing,
        @Valid
        @Nullable FarmingRecordRequests.PestControlDetailRequest pestControl,
        @Valid
        @Nullable FarmingRecordRequests.WeedingDetailRequest weeding,
        @Valid
        @Nullable FarmingRecordRequests.HarvestDetailRequest harvest
    ) {
        @Nullable public UUID getFarmId() { return farmId; }
        @Nullable public UUID getCropId() { return cropId; }
        @Nullable public WorkType getWorkType() { return workType; }
        @Nullable public LocalDateTime getWorkedAt() { return workedAt; }
        @Nullable public String getWeatherCondition() { return weatherCondition; }
        @Nullable public Integer getWeatherTemperature() { return weatherTemperature; }
        @Nullable public String getMemo() { return memo; }
        @Nullable public FarmingRecordRequests.PlantingDetailRequest getPlanting() { return planting; }
        @Nullable public FarmingRecordRequests.WateringDetailRequest getWatering() { return watering; }
        @Nullable public FarmingRecordRequests.FertilizingDetailRequest getFertilizing() { return fertilizing; }
        @Nullable public FarmingRecordRequests.PestControlDetailRequest getPestControl() { return pestControl; }
        @Nullable public FarmingRecordRequests.WeedingDetailRequest getWeeding() { return weeding; }
        @Nullable public FarmingRecordRequests.HarvestDetailRequest getHarvest() { return harvest; }
        public VoiceRecordCandidate toCandidate() {
            return new VoiceRecordCandidate(farmId, cropId, workType, workedAt, weatherCondition, weatherTemperature, memo,
                planting == null ? null : planting.toCommand(),
                watering == null ? null : watering.toCommand(),
                fertilizing == null ? null : fertilizing.toCommand(),
                pestControl == null ? null : pestControl.toCommand(),
                weeding == null ? null : weeding.toCommand(),
                harvest == null ? null : harvest.toCommand());
        }
    }
}
