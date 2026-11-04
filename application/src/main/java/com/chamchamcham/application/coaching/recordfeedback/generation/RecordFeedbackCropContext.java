package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackCropContext(
    @NonNull UUID cropId,
    @NonNull String name,
    @NonNull CropUsePartCategory usePartCategory
) {
    public RecordFeedbackCropContext {
        Objects.requireNonNull(cropId, "cropId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(usePartCategory, "usePartCategory");
    }
    @NonNull public UUID getCropId() { return cropId; }
    @NonNull public String getName() { return name; }
    @NonNull public CropUsePartCategory getUsePartCategory() { return usePartCategory; }
}
