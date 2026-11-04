package com.chamchamcham.application.farming;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.farming.WorkType;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class DefaultFarmingRecordDetailValidator implements FarmingRecordDetailValidator {
    @Override
    public void validate(FarmingRecordDetailPayload payload) {
        Objects.requireNonNull(payload, "payload");
        switch (payload.getWorkType()) {
            case PLANTING -> {
                requireDetail(payload.getWorkType(), payload.getPlanting());
                validatePlanting(payload.getPlanting());
            }
            case FERTILIZING -> requireDetail(payload.getWorkType(), payload.getFertilizing());
            case PEST_CONTROL -> requireDetail(payload.getWorkType(), payload.getPestControl());
            case HARVEST -> {
                requireDetail(payload.getWorkType(), payload.getHarvest());
                validateHarvest(payload.getHarvest());
            }
            case WATERING, WEEDING, PRUNING, ETC -> {}
        }
    }

    private void requireDetail(WorkType workType, Object detail) {
        if (!workType.getDetailRequired()) throw new IllegalStateException("WorkType " + workType + " is not marked as detailRequired");
        if (detail == null) throw new BusinessException(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED);
    }

    // SEED는 씨앗 수량만, SEEDLING은 모종 수량과 선택 번식 방법만 허용한다.
    private void validatePlanting(FarmingRecordCommand.PlantingDetail detail) {
        if (detail == null) return;
        switch (detail.getPlantingMethod()) {
            case SEED -> {
                if (detail.getSeedAmount() == null || detail.getSeedlingCount() != null
                        || detail.getSeedlingUnit() != null || detail.getPropagationMethod() != null) {
                    throw new BusinessException(ErrorCode.FARMING_RECORD_INVALID_DETAIL);
                }
            }
            case SEEDLING -> {
                if (detail.getSeedlingCount() == null || detail.getSeedAmount() != null || detail.getSeedAmountUnit() != null) {
                    throw new BusinessException(ErrorCode.FARMING_RECORD_INVALID_DETAIL);
                }
            }
        }
    }

    // 모르는 수확량은 NULL로 저장하며 실제 수량 0과 구분한다.
    private void validateHarvest(FarmingRecordCommand.HarvestDetail detail) {
        if (detail == null) return;
        if ((!detail.getAmountUnknown() && detail.getHarvestAmount() == null)
                || (detail.getAmountUnknown() && detail.getHarvestAmount() != null)) {
            throw new BusinessException(ErrorCode.FARMING_RECORD_INVALID_DETAIL);
        }
    }
}
