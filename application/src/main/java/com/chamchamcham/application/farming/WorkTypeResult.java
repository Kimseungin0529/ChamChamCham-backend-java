package com.chamchamcham.application.farming;

import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;

public final class WorkTypeResult {
    private WorkTypeResult() {}

    public record WorkTypeSummary(
        @NonNull String code,
        @NonNull String label,
        boolean detailRequired,
        @NonNull List<FieldSummary> fields
    ) {
        public WorkTypeSummary {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(fields, "fields");
        }

        @NonNull
        public String getCode() { return code; }
        @NonNull
        public String getLabel() { return label; }
        public boolean getDetailRequired() { return detailRequired; }
        @NonNull
        public List<FieldSummary> getFields() { return fields; }
    }

    public record FieldSummary(
        @NonNull String name,
        @NonNull FieldValueType type,
        boolean required,
        @NonNull List<EnumOptionSummary> options
    ) {
        public FieldSummary {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(options, "options");
        }

        @NonNull
        public String getName() { return name; }
        @NonNull
        public FieldValueType getType() { return type; }
        public boolean getRequired() { return required; }
        @NonNull
        public List<EnumOptionSummary> getOptions() { return options; }
    }

    public record EnumOptionSummary(
        @NonNull String code,
        @NonNull String label
    ) {
        public EnumOptionSummary {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
        }

        @NonNull
        public String getCode() { return code; }
        @NonNull
        public String getLabel() { return label; }
    }
}
