package com.chamchamcham.application.member;

import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public record MemberWithdrawalCommitted(
    @NonNull UUID memberId,
    @NonNull List<String> cloudinaryPublicIds
) {
    public MemberWithdrawalCommitted {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(cloudinaryPublicIds, "cloudinaryPublicIds");
    }

    @NonNull
    public UUID getMemberId() { return memberId; }
    @NonNull
    public List<String> getCloudinaryPublicIds() { return cloudinaryPublicIds; }

}
