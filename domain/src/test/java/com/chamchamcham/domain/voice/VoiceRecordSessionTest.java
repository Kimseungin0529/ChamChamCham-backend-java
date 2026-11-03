package com.chamchamcham.domain.voice;

import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.member.Member;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class VoiceRecordSessionTest {
    private final Member member = mock(Member.class);
    private final FarmingRecord record = mock(FarmingRecord.class);

    @Test
    void createdSessionWaitsForConfirmationAndStoresTranscript() {
        var session = new VoiceRecordSession(member, VoiceSessionStatus.CREATED);
        assertThat(session.getDraftRecord()).isNull();
        assertThat(session.getTranscript()).isNull();
        assertThat(session.getConfirmedAt()).isNull();
        session.markWaitingConfirmation("관수 기록");
        assertThat(session.getStatus()).isEqualTo(VoiceSessionStatus.WAITING_CONFIRMATION);
        assertThat(session.getTranscript()).isEqualTo("관수 기록");
        assertThatThrownBy(() -> session.markWaitingConfirmation("changed")).isInstanceOf(IllegalStateException.class);
        assertThat(session.getTranscript()).isEqualTo("관수 기록");
    }

    @Test
    void confirmStoresDraftAndTimeAndCompletedSessionCannotCancel() {
        var session = new VoiceRecordSession(member, VoiceSessionStatus.CREATED);
        var at = LocalDateTime.of(2026, 7, 1, 9, 0);
        session.markWaitingConfirmation("관수 기록");
        session.confirm(record, at);
        assertThat(session.getStatus()).isEqualTo(VoiceSessionStatus.COMPLETED);
        assertThat(session.getDraftRecord()).isSameAs(record);
        assertThat(session.getConfirmedAt()).isEqualTo(at);
        assertThatThrownBy(session::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.confirm(record, at.plusHours(1))).isInstanceOf(IllegalStateException.class);
        assertThat(session.getStatus()).isEqualTo(VoiceSessionStatus.COMPLETED);
        assertThat(session.getConfirmedAt()).isEqualTo(at);
    }

    @Test
    void invalidConfirmationLeavesCreatedSessionUnchanged() {
        var session = new VoiceRecordSession(member, VoiceSessionStatus.CREATED);
        assertThatThrownBy(() -> session.confirm(record, LocalDateTime.now())).isInstanceOf(IllegalStateException.class);
        assertThat(session.getStatus()).isEqualTo(VoiceSessionStatus.CREATED);
        assertThat(session.getDraftRecord()).isNull();
        assertThat(session.getConfirmedAt()).isNull();
    }

    @Test
    void everyNonCompletedStatusCanBeCancelledRepeatedly() {
        for (VoiceSessionStatus status : VoiceSessionStatus.values()) {
            if (status == VoiceSessionStatus.COMPLETED) continue;
            var session = new VoiceRecordSession(member, status);
            session.cancel();
            session.cancel();
            assertThat(session.getStatus()).isEqualTo(VoiceSessionStatus.CANCELLED);
        }
    }

    @Test
    void canonicalConstructorsPreserveNullableFieldsAndTurnValues() {
        UUID id = UUID.randomUUID();
        var at = LocalDateTime.of(2026, 7, 1, 9, 0);
        var session = new VoiceRecordSession(id, member, record, VoiceSessionStatus.COMPLETED, "기록", at);
        var turn = new VoiceRecordTurn(id, session, VoiceTurnRole.USER, "관수", null);
        assertThat(session.getId()).isEqualTo(id);
        assertThat(session.getDraftRecord()).isSameAs(record);
        assertThat(session.getConfirmedAt()).isEqualTo(at);
        assertThat(turn.getId()).isEqualTo(id);
        assertThat(turn.getSession()).isSameAs(session);
        assertThat(turn.getRole()).isEqualTo(VoiceTurnRole.USER);
        assertThat(turn.getContent()).isEqualTo("관수");
        assertThat(turn.getExtractedFields()).isNull();
    }
}
