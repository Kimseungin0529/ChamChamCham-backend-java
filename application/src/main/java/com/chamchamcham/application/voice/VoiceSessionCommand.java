package com.chamchamcham.application.voice;

import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.domain.voice.VoiceTurnRole;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class VoiceSessionCommand {
    private VoiceSessionCommand() {}

    public record Create(
        @NonNull UUID memberId
    ) {
        public Create {
            Objects.requireNonNull(memberId, "memberId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
    }

    public record SubmitTurns(
        @NonNull UUID memberId,
        @NonNull UUID sessionId,
        @NonNull List<TurnInput> turns,
        @NonNull VoiceRecordCandidate candidate
    ) {
        public SubmitTurns {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(turns, "turns");
            Objects.requireNonNull(candidate, "candidate");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getSessionId() { return sessionId; }
        @NonNull
        public List<TurnInput> getTurns() { return turns; }
        @NonNull
        public VoiceRecordCandidate getCandidate() { return candidate; }
    }

    public record TurnInput(
        @NonNull VoiceTurnRole role,
        @NonNull String content,
        @Nullable String extractedFields
    ) {
        public TurnInput {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(content, "content");
        }

        @NonNull
        public VoiceTurnRole getRole() { return role; }
        @NonNull
        public String getContent() { return content; }
        @Nullable
        public String getExtractedFields() { return extractedFields; }
    }

    public record Confirm(
        @NonNull UUID memberId,
        @NonNull UUID sessionId,
        @NonNull FarmingRecordCommand.Create record
    ) {
        public Confirm {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(record, "record");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getSessionId() { return sessionId; }
        @NonNull
        public FarmingRecordCommand.Create getRecord() { return record; }
    }

    public record Cancel(
        @NonNull UUID memberId,
        @NonNull UUID sessionId
    ) {
        public Cancel {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(sessionId, "sessionId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getSessionId() { return sessionId; }
    }
}
