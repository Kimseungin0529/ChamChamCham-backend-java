package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PlantingFeedbackDetail.class, name = "PLANTING"),
    @JsonSubTypes.Type(value = WateringFeedbackDetail.class, name = "WATERING"),
    @JsonSubTypes.Type(value = FertilizingFeedbackDetail.class, name = "FERTILIZING"),
    @JsonSubTypes.Type(value = PestControlFeedbackDetail.class, name = "PEST_CONTROL"),
    @JsonSubTypes.Type(value = WeedingFeedbackDetail.class, name = "WEEDING"),
    @JsonSubTypes.Type(value = HarvestFeedbackDetail.class, name = "HARVEST"),
    @JsonSubTypes.Type(value = CommonFeedbackDetail.class, name = "COMMON")
})
public sealed interface RecordFeedbackWorkDetail permits PlantingFeedbackDetail, WateringFeedbackDetail, FertilizingFeedbackDetail, PestControlFeedbackDetail, WeedingFeedbackDetail, HarvestFeedbackDetail, CommonFeedbackDetail {}
