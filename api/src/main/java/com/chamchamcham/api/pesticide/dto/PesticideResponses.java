package com.chamchamcham.api.pesticide.dto;

import com.chamchamcham.application.pesticide.PesticideResult;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class PesticideResponses {
    private PesticideResponses() {}

    public record PesticideSummaryResponse(
        @NonNull UUID id,
        @NonNull String itemName,
        @NonNull String brandName,
        @Nullable String usageCategory,
        @Nullable String manufacturer
    ) {
        public PesticideSummaryResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(itemName, "itemName");
            Objects.requireNonNull(brandName, "brandName");
        }

        @NonNull public UUID getId() { return id; }
        @NonNull public String getItemName() { return itemName; }
        @NonNull public String getBrandName() { return brandName; }
        @Nullable public String getUsageCategory() { return usageCategory; }
        @Nullable public String getManufacturer() { return manufacturer; }

        public static PesticideSummaryResponse from(PesticideResult.Summary result) {
            return new PesticideSummaryResponse(
                result.getId(),
                result.getItemName(),
                result.getBrandName(),
                result.getUsageCategory(),
                result.getManufacturer()
            );
        }
    }

    public record PesticidePageResponse(
        @NonNull List<PesticideSummaryResponse> items,
        @Nullable String nextCursor
    ) {
        public PesticidePageResponse {
            Objects.requireNonNull(items, "items");
        }

        @NonNull public List<PesticideSummaryResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }

        public static PesticidePageResponse from(PesticideResult.Page result) {
            return new PesticidePageResponse(
                result.getItems().stream().map(PesticideSummaryResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                result.getNextCursor()
            );
        }
    }

    public record PestSummaryResponse(
        @NonNull UUID id,
        @NonNull String name
    ) {
        public PestSummaryResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
        }

        @NonNull public UUID getId() { return id; }
        @NonNull public String getName() { return name; }

        public static PestSummaryResponse from(PesticideResult.PestSummary result) {
            return new PestSummaryResponse(
                result.getId(),
                result.getName()
            );
        }
    }
}
