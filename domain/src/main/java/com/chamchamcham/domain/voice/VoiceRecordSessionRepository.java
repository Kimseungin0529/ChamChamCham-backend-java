package com.chamchamcham.domain.voice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

public interface VoiceRecordSessionRepository extends JpaRepository<VoiceRecordSession, UUID> {
    @Nullable
    VoiceRecordSession findByIdAndMemberId(UUID id, UUID memberId);
}
