package com.chamchamcham.api.voice.dto;

import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.voice.VoiceRecordCandidate;
import com.chamchamcham.application.voice.VoiceSessionResult;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.voice.VoiceSessionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class VoiceSessionResponses {
    private VoiceSessionResponses() {}

    public record CreatedResponse(
        @NonNull UUID sessionId,
        @NonNull String clientSecret,
        @NonNull LocalDateTime expiresAt,
        @NonNull String model,
        @NonNull List<FarmOptionResponse> farms,
        @NonNull Map<String, List<CropOptionResponse>> cropsByFarm,
        int maxRounds,
        int maxDurationSeconds
    ) {
        public CreatedResponse {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(clientSecret, "clientSecret");
            Objects.requireNonNull(expiresAt, "expiresAt");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(farms, "farms");
            Objects.requireNonNull(cropsByFarm, "cropsByFarm");
        }
        @NonNull public UUID getSessionId() { return sessionId; }
        @NonNull public String getClientSecret() { return clientSecret; }
        @NonNull public LocalDateTime getExpiresAt() { return expiresAt; }
        @NonNull public String getModel() { return model; }
        @NonNull public List<FarmOptionResponse> getFarms() { return farms; }
        @NonNull public Map<String, List<CropOptionResponse>> getCropsByFarm() { return cropsByFarm; }
        public int getMaxRounds() { return maxRounds; }
        public int getMaxDurationSeconds() { return maxDurationSeconds; }
        public static CreatedResponse from(VoiceSessionResult.Created result) {
            return new CreatedResponse(
                result.getSessionId(),
                result.getClientSecret(),
                result.getExpiresAt(),
                result.getModel(),
                result.getFarms().stream().map(f -> new FarmOptionResponse(f.getFarmId(), f.getName())).collect(Collectors.toCollection(ArrayList::new)),
                mapCrops(result.getCropsByFarm()),
                result.getMaxRounds(),
                result.getMaxDurationSeconds()
            );
        }
    }

    public record FarmOptionResponse(
        @NonNull UUID farmId,
        @NonNull String name
    ) {
        public FarmOptionResponse {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(name, "name");
        }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getName() { return name; }
    }

    public record CropOptionResponse(
        @NonNull UUID cropId,
        @NonNull String name
    ) {
        public CropOptionResponse {
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(name, "name");
        }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getName() { return name; }
    }

    public record ProcessedResponse(
        @NonNull UUID sessionId,
        @NonNull VoiceSessionStatus status,
        @NonNull CandidateResponse candidate,
        @NonNull List<String> missingFields
    ) {
        public ProcessedResponse {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(missingFields, "missingFields");
        }
        @NonNull public UUID getSessionId() { return sessionId; }
        @NonNull public VoiceSessionStatus getStatus() { return status; }
        @NonNull public CandidateResponse getCandidate() { return candidate; }
        @NonNull public List<String> getMissingFields() { return missingFields; }
        public static ProcessedResponse from(VoiceSessionResult.Processed result) {
            return new ProcessedResponse(
                result.getSessionId(),
                result.getStatus(),
                CandidateResponse.from(result.getCandidate()),
                result.getMissingFields()
            );
        }
    }

    public record CandidateResponse(
        @Nullable UUID farmId,
        @Nullable UUID cropId,
        @Nullable WorkType workType,
        @Nullable LocalDateTime workedAt,
        @Nullable String weatherCondition,
        @Nullable Integer weatherTemperature,
        @Nullable String memo,
        @Nullable FarmingRecordCommand.PlantingDetail planting,
        @Nullable FarmingRecordCommand.WateringDetail watering,
        @Nullable FarmingRecordCommand.FertilizingDetail fertilizing,
        @Nullable FarmingRecordCommand.PestControlDetail pestControl,
        @Nullable FarmingRecordCommand.WeedingDetail weeding,
        @Nullable FarmingRecordCommand.HarvestDetail harvest
    ) {
        @Nullable public UUID getFarmId() { return farmId; }
        @Nullable public UUID getCropId() { return cropId; }
        @Nullable public WorkType getWorkType() { return workType; }
        @Nullable public LocalDateTime getWorkedAt() { return workedAt; }
        @Nullable public String getWeatherCondition() { return weatherCondition; }
        @Nullable public Integer getWeatherTemperature() { return weatherTemperature; }
        @Nullable public String getMemo() { return memo; }
        @Nullable public FarmingRecordCommand.PlantingDetail getPlanting() { return planting; }
        @Nullable public FarmingRecordCommand.WateringDetail getWatering() { return watering; }
        @Nullable public FarmingRecordCommand.FertilizingDetail getFertilizing() { return fertilizing; }
        @Nullable public FarmingRecordCommand.PestControlDetail getPestControl() { return pestControl; }
        @Nullable public FarmingRecordCommand.WeedingDetail getWeeding() { return weeding; }
        @Nullable public FarmingRecordCommand.HarvestDetail getHarvest() { return harvest; }
        public static CandidateResponse from(VoiceRecordCandidate candidate) {
            return new CandidateResponse(
                candidate.getFarmId(),
                candidate.getCropId(),
                candidate.getWorkType(),
                candidate.getWorkedAt(),
                candidate.getWeatherCondition(),
                candidate.getWeatherTemperature(),
                candidate.getMemo(),
                candidate.getPlanting(),
                candidate.getWatering(),
                candidate.getFertilizing(),
                candidate.getPestControl(),
                candidate.getWeeding(),
                candidate.getHarvest()
            );
        }
    }

    public record ConfirmedResponse(
        @NonNull UUID sessionId,
        @NonNull UUID recordId,
        @NonNull WorkType workType
    ) {
        public ConfirmedResponse {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(recordId, "recordId");
            Objects.requireNonNull(workType, "workType");
        }
        @NonNull public UUID getSessionId() { return sessionId; }
        @NonNull public UUID getRecordId() { return recordId; }
        @NonNull public WorkType getWorkType() { return workType; }
        public static ConfirmedResponse from(VoiceSessionResult.Confirmed result) {
            return new ConfirmedResponse(
                result.getSessionId(),
                result.getRecordId(),
                result.getWorkType()
            );
        }
    }

    public record CancelledResponse(
        @NonNull UUID sessionId,
        @NonNull VoiceSessionStatus status
    ) {
        public CancelledResponse {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(status, "status");
        }
        @NonNull public UUID getSessionId() { return sessionId; }
        @NonNull public VoiceSessionStatus getStatus() { return status; }
        public static CancelledResponse from(VoiceSessionResult.Cancelled result) {
            return new CancelledResponse(
                result.getSessionId(),
                result.getStatus()
            );
        }
    }

    private static Map<String, List<CropOptionResponse>> mapCrops(Map<String, List<com.chamchamcham.application.voice.CropOption>> source) {
        var result = new java.util.LinkedHashMap<String, List<CropOptionResponse>>();
        source.forEach((key,
            crops) -> result.put(key,
            crops.stream().map(c -> new CropOptionResponse(c.getCropId(),
            c.getName())).collect(Collectors.toCollection(ArrayList::new))));
        return result;
    }
}
