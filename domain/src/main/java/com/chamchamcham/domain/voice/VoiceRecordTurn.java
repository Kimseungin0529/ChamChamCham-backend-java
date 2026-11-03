package com.chamchamcham.domain.voice;

import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "voice_record_turn")
public class VoiceRecordTurn extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private VoiceRecordSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VoiceTurnRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "extracted_fields", columnDefinition = "text")
    private String extractedFields;

    protected VoiceRecordTurn() {}

    public VoiceRecordTurn(
            @Nullable UUID id,
            VoiceRecordSession session,
            VoiceTurnRole role,
            String content,
            @Nullable String extractedFields
    ) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
        this.id = id;
        this.session = session;
        this.role = role;
        this.content = content;
        this.extractedFields = extractedFields;
    }

    @Nullable
    public UUID getId() { return id; }
    public VoiceRecordSession getSession() { return session; }
    public VoiceTurnRole getRole() { return role; }
    public String getContent() { return content; }
    @Nullable
    public String getExtractedFields() { return extractedFields; }

    public VoiceRecordTurn(VoiceRecordSession session, VoiceTurnRole role, String content) {
        this(null, session, role, content, null);
    }

    public VoiceRecordTurn(@Nullable UUID id, VoiceRecordSession session, VoiceTurnRole role, String content) {
        this(id, session, role, content, null);
    }

}
