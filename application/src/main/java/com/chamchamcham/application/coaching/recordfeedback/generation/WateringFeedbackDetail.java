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

public record WateringFeedbackDetail(
    @Nullable IrrigationAmount irrigationAmount,
    @Nullable IrrigationMethod irrigationMethod
) implements RecordFeedbackWorkDetail {
    public WateringFeedbackDetail {

    }
    @Nullable public IrrigationAmount getIrrigationAmount() { return irrigationAmount; }
    @Nullable public IrrigationMethod getIrrigationMethod() { return irrigationMethod; }
}
