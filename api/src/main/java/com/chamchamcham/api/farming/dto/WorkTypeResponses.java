package com.chamchamcham.api.farming.dto;

import com.chamchamcham.application.farming.WorkTypeResult;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

public final class WorkTypeResponses {
    private WorkTypeResponses() {}

    public record WorkTypeResponse(
        @NonNull String code,
        @NonNull String label,
        boolean detailRequired,
        @NonNull List<FieldResponse> fields
    ) {
        public WorkTypeResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(fields, "fields");
        }
        @NonNull public String getCode() { return code; }
        @NonNull public String getLabel() { return label; }
        public boolean getDetailRequired() { return detailRequired; }
        @NonNull public List<FieldResponse> getFields() { return fields; }
        public static WorkTypeResponse from(WorkTypeResult.WorkTypeSummary result) {
            return new WorkTypeResponse(
                result.getCode(),
                result.getLabel(),
                result.getDetailRequired(),
                result.getFields().stream().map(FieldResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record FieldResponse(
        @NonNull String name,
        @NonNull String type,
        boolean required,
        @NonNull List<EnumOptionResponse> options
    ) {
        public FieldResponse {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(options, "options");
        }
        @NonNull public String getName() { return name; }
        @NonNull public String getType() { return type; }
        public boolean getRequired() { return required; }
        @NonNull public List<EnumOptionResponse> getOptions() { return options; }
        public static FieldResponse from(WorkTypeResult.FieldSummary result) {
            return new FieldResponse(
                result.getName(),
                result.getType().name(),
                result.getRequired(),
                result.getOptions().stream().map(EnumOptionResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record EnumOptionResponse(
        @NonNull String code,
        @NonNull String label
    ) {
        public EnumOptionResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
        }
        @NonNull public String getCode() { return code; }
        @NonNull public String getLabel() { return label; }
        public static EnumOptionResponse from(WorkTypeResult.EnumOptionSummary result) {
            return new EnumOptionResponse(
                result.getCode(),
                result.getLabel()
            );
        }
    }
}
