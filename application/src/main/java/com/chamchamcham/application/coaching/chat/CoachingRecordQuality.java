package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagSourceType;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingRecordQuality(
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ScoreDefault.class) CoachingRecordQualityScore score,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = MissingOrWeakFieldsDefault.class) List<String> missingOrWeakFields,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CommentDefault.class) String comment
) {
    public CoachingRecordQuality {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(missingOrWeakFields, "missingOrWeakFields");
        Objects.requireNonNull(comment, "comment");
    }
    @NonNull public CoachingRecordQualityScore getScore() { return score; }
    @NonNull public List<String> getMissingOrWeakFields() { return missingOrWeakFields; }
    @NonNull public String getComment() { return comment; }
    public CoachingRecordQuality() { this(CoachingRecordQualityScore.UNKNOWN, java.util.Collections.emptyList(), ""); }
    public static final class ScoreDefault extends com.fasterxml.jackson.databind.JsonDeserializer<CoachingRecordQualityScore> {
        @Override public CoachingRecordQualityScore deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, CoachingRecordQualityScore.class);
        }
        @Override public CoachingRecordQualityScore getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return CoachingRecordQualityScore.UNKNOWN; }
    }
    public static final class MissingOrWeakFieldsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<String>> {
        @Override public List<String> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
        }
        @Override public List<String> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
    public static final class CommentDefault extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
        @Override public String deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, String.class);
        }
        @Override public String getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return ""; }
    }
}
