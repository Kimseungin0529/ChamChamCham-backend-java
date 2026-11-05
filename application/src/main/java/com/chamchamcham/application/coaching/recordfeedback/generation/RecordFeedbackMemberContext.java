package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackMemberContext(
    @NonNull UUID memberId,
    @Nullable Integer experienceLevel,
    @Nullable ManagementType managementType
) {
    public RecordFeedbackMemberContext {
        Objects.requireNonNull(memberId, "memberId");
    }
    @NonNull public UUID getMemberId() { return memberId; }
    @Nullable public Integer getExperienceLevel() { return experienceLevel; }
    @Nullable public ManagementType getManagementType() { return managementType; }
}
