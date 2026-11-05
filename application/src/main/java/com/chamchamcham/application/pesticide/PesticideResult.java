package com.chamchamcham.application.pesticide;

import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.Pesticide;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class PesticideResult {
    private PesticideResult() {}

    public record Summary(
        @NonNull UUID id,
        @NonNull String itemName,
        @NonNull String brandName,
        @Nullable String usageCategory,
        @Nullable String manufacturer
    ) {
        public Summary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(itemName, "itemName");
            Objects.requireNonNull(brandName, "brandName");
        }

        @NonNull
        public UUID getId() { return id; }
        public static Summary from(com.chamchamcham.domain.pesticide.Pesticide pesticide) {
            Objects.requireNonNull(pesticide, "pesticide");
            if (pesticide.getId() == null) throw new IllegalArgumentException("Persisted pesticide id is required");
            return new Summary(pesticide.getId(), pesticide.getItemName(), pesticide.getBrandName(), pesticide.getUsageCategory(), pesticide.getManufacturer());
        }

        @NonNull
        public String getItemName() { return itemName; }
        @NonNull
        public String getBrandName() { return brandName; }
        @Nullable
        public String getUsageCategory() { return usageCategory; }
        @Nullable
        public String getManufacturer() { return manufacturer; }
    }

    public record Page(
        @NonNull List<Summary> items,
        @Nullable String nextCursor
    ) {
        public Page {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<Summary> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
    }

    public record PestSummary(
        @NonNull UUID id,
        @NonNull String name
    ) {
        public PestSummary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
        }

        @NonNull
        public UUID getId() { return id; }
        public static PestSummary from(com.chamchamcham.domain.pesticide.Pest pest) {
            Objects.requireNonNull(pest, "pest");
            if (pest.getId() == null) throw new IllegalArgumentException("Persisted pest id is required");
            return new PestSummary(pest.getId(), pest.getName());
        }

        @NonNull
        public String getName() { return name; }
    }
}
