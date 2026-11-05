package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.FertilizingMethod;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PropagationMethod;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SeedlingUnit;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PestControlFeedbackDetail(
    @NonNull String pesticideName,
    @NonNull BigDecimal pesticideAmount,
    @NonNull PesticideAmountUnit pesticideAmountUnit,
    @NonNull BigDecimal totalSprayAmount,
    @NonNull String totalSprayAmountUnit,
    @Nullable String pestName
) implements RecordFeedbackWorkDetail {
    public PestControlFeedbackDetail {
        Objects.requireNonNull(pesticideName, "pesticideName");
        Objects.requireNonNull(pesticideAmount, "pesticideAmount");
        Objects.requireNonNull(pesticideAmountUnit, "pesticideAmountUnit");
        Objects.requireNonNull(totalSprayAmount, "totalSprayAmount");
        Objects.requireNonNull(totalSprayAmountUnit, "totalSprayAmountUnit");
    }
    @NonNull public String getPesticideName() { return pesticideName; }
    @NonNull public BigDecimal getPesticideAmount() { return pesticideAmount; }
    @NonNull public PesticideAmountUnit getPesticideAmountUnit() { return pesticideAmountUnit; }
    @NonNull public BigDecimal getTotalSprayAmount() { return totalSprayAmount; }
    @NonNull public String getTotalSprayAmountUnit() { return totalSprayAmountUnit; }
    @Nullable public String getPestName() { return pestName; }
}
