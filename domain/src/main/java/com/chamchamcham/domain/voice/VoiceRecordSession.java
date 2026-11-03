package com.chamchamcham.domain.voice;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.member.Member;
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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "voice_record_session")
public class VoiceRecordSession extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_record_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private FarmingRecord draftRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VoiceSessionStatus status;

    @Column(columnDefinition = "text")
    private String transcript;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    protected VoiceRecordSession() {}

    public VoiceRecordSession(
            @Nullable UUID id,
            Member member,
            @Nullable FarmingRecord draftRecord,
            VoiceSessionStatus status,
            @Nullable String transcript,
            @Nullable LocalDateTime confirmedAt
    ) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(status, "status");
        this.id = id;
        this.member = member;
        this.draftRecord = draftRecord;
        this.status = status;
        this.transcript = transcript;
        this.confirmedAt = confirmedAt;
    }

    @Nullable
    public UUID getId() { return id; }
    public Member getMember() { return member; }
    @Nullable
    public FarmingRecord getDraftRecord() { return draftRecord; }
    public void setDraftRecord(@Nullable FarmingRecord draftRecord) { this.draftRecord = draftRecord; }
    public VoiceSessionStatus getStatus() { return status; }
    public void setStatus(VoiceSessionStatus status) { this.status = Objects.requireNonNull(status, "status"); }
    @Nullable
    public String getTranscript() { return transcript; }
    public void setTranscript(@Nullable String transcript) { this.transcript = transcript; }
    @Nullable
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(@Nullable LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public VoiceRecordSession(Member member, VoiceSessionStatus status) {
        this(null, member, null, status, null, null);
    }

    public VoiceRecordSession(@Nullable UUID id, Member member, VoiceSessionStatus status) {
        this(id, member, null, status, null, null);
    }

    public void markWaitingConfirmation(String transcript) {
        Objects.requireNonNull(transcript, "transcript");
        if (status != VoiceSessionStatus.CREATED) throw new IllegalStateException("session " + id + " is not in CREATED state");
        this.transcript = transcript;
        status = VoiceSessionStatus.WAITING_CONFIRMATION;
    }

    public void confirm(FarmingRecord record, LocalDateTime confirmedAt) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(confirmedAt, "confirmedAt");
        if (status != VoiceSessionStatus.WAITING_CONFIRMATION) throw new IllegalStateException("session " + id + " is not in WAITING_CONFIRMATION state");
        draftRecord = record;
        this.confirmedAt = confirmedAt;
        status = VoiceSessionStatus.COMPLETED;
    }

    public void cancel() {
        if (status == VoiceSessionStatus.COMPLETED) throw new IllegalStateException("session " + id + " is already COMPLETED");
        status = VoiceSessionStatus.CANCELLED;
    }
}
