package com.chamchamcham.application.voice;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.voice.VoiceSessionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.NonNull;

public final class VoiceSessionResult {
    private VoiceSessionResult() {}

    public record Created(
        @NonNull UUID sessionId,
        @NonNull String clientSecret,
        @NonNull LocalDateTime expiresAt,
        @NonNull String model,
        @NonNull List<FarmOption> farms,
        @NonNull Map<String, List<CropOption>> cropsByFarm,
        int maxRounds,
        int maxDurationSeconds
    ) {
        public Created {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(clientSecret, "clientSecret");
            Objects.requireNonNull(expiresAt, "expiresAt");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(farms, "farms");
            Objects.requireNonNull(cropsByFarm, "cropsByFarm");
        }

        @NonNull
        public UUID getSessionId() { return sessionId; }
        @NonNull
        public String getClientSecret() { return clientSecret; }
        @NonNull
        public LocalDateTime getExpiresAt() { return expiresAt; }
        @NonNull
        public String getModel() { return model; }
        @NonNull
        public List<FarmOption> getFarms() { return farms; }
        @NonNull
        public Map<String, List<CropOption>> getCropsByFarm() { return cropsByFarm; }
        public int getMaxRounds() { return maxRounds; }
        public int getMaxDurationSeconds() { return maxDurationSeconds; }
    }

    public record Processed(
        @NonNull UUID sessionId,
        @NonNull VoiceSessionStatus status,
        @NonNull VoiceRecordCandidate candidate,
        @NonNull List<String> missingFields
    ) {
        public Processed {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(missingFields, "missingFields");
        }

        @NonNull
        public UUID getSessionId() { return sessionId; }
        @NonNull
        public VoiceSessionStatus getStatus() { return status; }
        @NonNull
        public VoiceRecordCandidate getCandidate() { return candidate; }
        @NonNull
        public List<String> getMissingFields() { return missingFields; }
    }

    public record Confirmed(
        @NonNull UUID sessionId,
        @NonNull UUID recordId,
        @NonNull WorkType workType
    ) {
        public Confirmed {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(recordId, "recordId");
            Objects.requireNonNull(workType, "workType");
        }

        @NonNull
        public UUID getSessionId() { return sessionId; }
        @NonNull
        public UUID getRecordId() { return recordId; }
        @NonNull
        public WorkType getWorkType() { return workType; }
    }

    public record Cancelled(
        @NonNull UUID sessionId,
        @NonNull VoiceSessionStatus status
    ) {
        public Cancelled {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(status, "status");
        }

        @NonNull
        public UUID getSessionId() { return sessionId; }
        @NonNull
        public VoiceSessionStatus getStatus() { return status; }
    }
}
