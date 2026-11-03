package com.chamchamcham.application.member;

import com.chamchamcham.application.media.ImageUploader;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalAfterCommitListenerTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final List<String> publicIds = List.of("profile/image", "community/image");
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private ImageUploader imageUploader;

    @Test
    void afterCommitRemovesRefreshTokenAndDeletesCloudinaryOriginalsOnce() {
        var listener = new MemberWithdrawalAfterCommitListener(refreshTokenRepository, imageUploader);
        listener.handle(new MemberWithdrawalCommitted(memberId, publicIds));
        verify(refreshTokenRepository).delete(memberId);
        verify(imageUploader).delete("profile/image");
        verify(imageUploader).delete("community/image");
    }

    @Test
    void refreshCleanupFailureDoesNotBlockCloudinaryTasks() {
        var listener = new MemberWithdrawalAfterCommitListener(refreshTokenRepository, imageUploader);
        doThrow(new IllegalStateException("redis down")).when(refreshTokenRepository).delete(memberId);
        listener.handle(new MemberWithdrawalCommitted(memberId, publicIds));
        verify(imageUploader).delete("profile/image");
        verify(imageUploader).delete("community/image");
    }

    @Test
    void oneCloudinaryFailureDoesNotBlockRemainingDeletes() {
        var listener = new MemberWithdrawalAfterCommitListener(refreshTokenRepository, imageUploader);
        doThrow(new IllegalStateException("provider down")).when(imageUploader).delete("profile/image");
        listener.handle(new MemberWithdrawalCommitted(memberId, publicIds));
        verify(imageUploader).delete("community/image");
    }

    @Test
    void cleanupErrorDoesNotBlockRemainingDeletes() {
        var listener = new MemberWithdrawalAfterCommitListener(refreshTokenRepository, imageUploader);
        doThrow(new AssertionError("cleanup error")).when(imageUploader).delete("profile/image");
        listener.handle(new MemberWithdrawalCommitted(memberId, publicIds));
        verify(imageUploader).delete("community/image");
    }
}
