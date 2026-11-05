package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackRecordContext(
    @NonNull UUID recordId,
     long sourceRevision,
    @NonNull LocalDateTime workedAt,
    @NonNull WorkType workType,
    @NonNull RecordFeedbackWorkDetail detail,
    @NonNull String recordedWeatherCondition,
     int recordedTemperatureC,
    @NonNull String memo,
     int photoCount
) {
    public RecordFeedbackRecordContext {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(workedAt, "workedAt");
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(recordedWeatherCondition, "recordedWeatherCondition");
        Objects.requireNonNull(memo, "memo");
    }
    @NonNull public UUID getRecordId() { return recordId; }
    public long getSourceRevision() { return sourceRevision; }
    @NonNull public LocalDateTime getWorkedAt() { return workedAt; }
    @NonNull public WorkType getWorkType() { return workType; }
    @NonNull public RecordFeedbackWorkDetail getDetail() { return detail; }
    @NonNull public String getRecordedWeatherCondition() { return recordedWeatherCondition; }
    public int getRecordedTemperatureC() { return recordedTemperatureC; }
    @NonNull public String getMemo() { return memo; }
    public int getPhotoCount() { return photoCount; }
}
