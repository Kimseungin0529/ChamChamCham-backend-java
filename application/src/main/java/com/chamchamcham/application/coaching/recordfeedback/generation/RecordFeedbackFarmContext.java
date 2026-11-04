package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackFarmContext(
    @NonNull UUID farmId,
    @NonNull String name,
    @NonNull String roadAddress,
    @Nullable Double latitude,
    @Nullable Double longitude
) {
    public RecordFeedbackFarmContext {
        Objects.requireNonNull(farmId, "farmId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(roadAddress, "roadAddress");
    }
    @NonNull public UUID getFarmId() { return farmId; }
    @NonNull public String getName() { return name; }
    @NonNull public String getRoadAddress() { return roadAddress; }
    @Nullable public Double getLatitude() { return latitude; }
    @Nullable public Double getLongitude() { return longitude; }
}
