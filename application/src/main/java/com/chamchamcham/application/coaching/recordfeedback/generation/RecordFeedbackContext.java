package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackContext(
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SchemaVersionDefault.class) String schemaVersion,
    @NonNull RecordFeedbackMemberContext member,
    @NonNull RecordFeedbackFarmContext farm,
    @NonNull RecordFeedbackCropContext crop,
    @NonNull RecordFeedbackRecordContext record,
    @Nullable RecordFeedbackLiveWeather weather,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = WarningsDefault.class) List<String> warnings
) {
    public RecordFeedbackContext {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(farm, "farm");
        Objects.requireNonNull(crop, "crop");
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(warnings, "warnings");
    }
    @NonNull public String getSchemaVersion() { return schemaVersion; }
    @NonNull public RecordFeedbackMemberContext getMember() { return member; }
    @NonNull public RecordFeedbackFarmContext getFarm() { return farm; }
    @NonNull public RecordFeedbackCropContext getCrop() { return crop; }
    @NonNull public RecordFeedbackRecordContext getRecord() { return record; }
    @Nullable public RecordFeedbackLiveWeather getWeather() { return weather; }
    @NonNull public List<String> getWarnings() { return warnings; }
    public static final String RECORD_FEEDBACK_CONTEXT_SCHEMA_VERSION = "record-feedback-context.v2";
    public String recordCitationId() { return "record:" + record.getRecordId(); }
    public static final class SchemaVersionDefault extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
        @Override public String deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, String.class);
        }
        @Override public String getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return RecordFeedbackContext.RECORD_FEEDBACK_CONTEXT_SCHEMA_VERSION; }
    }
    public static final class WarningsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<String>> {
        @Override public List<String> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
        }
        @Override public List<String> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
}
