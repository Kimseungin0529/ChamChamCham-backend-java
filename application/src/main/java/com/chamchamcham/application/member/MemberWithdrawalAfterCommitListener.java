package com.chamchamcham.application.member;

import com.chamchamcham.application.media.ImageUploader;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.Objects;

@Component
public class MemberWithdrawalAfterCommitListener {
    private final Logger logger = LoggerFactory.getLogger(MemberWithdrawalAfterCommitListener.class);
    private final RefreshTokenRepository refreshTokenRepository;
    private final ImageUploader imageUploader;

    public MemberWithdrawalAfterCommitListener(RefreshTokenRepository refreshTokenRepository, ImageUploader imageUploader) {
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "refreshTokenRepository");
        this.imageUploader = Objects.requireNonNull(imageUploader, "imageUploader");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemberWithdrawalCommitted event) {
        Objects.requireNonNull(event, "event");
        try {
            refreshTokenRepository.delete(event.memberId());
        } catch (Throwable failure) {
            logger.warn("Refresh token cleanup failed after member withdrawal");
        }
        for (var publicId : event.cloudinaryPublicIds()) {
            try {
                imageUploader.delete(publicId);
            } catch (Throwable failure) {
                logger.warn("Cloudinary deletion failed after member withdrawal");
            }
        }
    }
}
