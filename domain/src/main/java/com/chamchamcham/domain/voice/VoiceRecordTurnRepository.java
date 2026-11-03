package com.chamchamcham.domain.voice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceRecordTurnRepository extends JpaRepository<VoiceRecordTurn, UUID> {

}
