package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.weather.WeatherFallback;
import com.chamchamcham.domain.farming.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Component
public class RecordFeedbackContextAssembler {
    private final FarmingRecordRepository recordRepository;
    private final FarmingRecordMediaRepository mediaRepository;
    private final PlantingRecordRepository plantingRecordRepository;
    private final WateringRecordRepository wateringRecordRepository;
    private final FertilizingRecordRepository fertilizingRecordRepository;
    private final PestControlRecordRepository pestControlRecordRepository;
    private final WeedingRecordRepository weedingRecordRepository;
    private final HarvestRecordRepository harvestRecordRepository;
    private final RecordFeedbackWeatherPort weatherPort;
    private final Clock clock;
    public RecordFeedbackContextAssembler(FarmingRecordRepository recordRepository, FarmingRecordMediaRepository mediaRepository, PlantingRecordRepository plantingRecordRepository, WateringRecordRepository wateringRecordRepository, FertilizingRecordRepository fertilizingRecordRepository, PestControlRecordRepository pestControlRecordRepository, WeedingRecordRepository weedingRecordRepository, HarvestRecordRepository harvestRecordRepository, RecordFeedbackWeatherPort weatherPort, Clock clock) {
        this.recordRepository = Objects.requireNonNull(recordRepository); this.mediaRepository = Objects.requireNonNull(mediaRepository); this.plantingRecordRepository = Objects.requireNonNull(plantingRecordRepository); this.wateringRecordRepository = Objects.requireNonNull(wateringRecordRepository); this.fertilizingRecordRepository = Objects.requireNonNull(fertilizingRecordRepository); this.pestControlRecordRepository = Objects.requireNonNull(pestControlRecordRepository); this.weedingRecordRepository = Objects.requireNonNull(weedingRecordRepository); this.harvestRecordRepository = Objects.requireNonNull(harvestRecordRepository); this.weatherPort = Objects.requireNonNull(weatherPort); this.clock = Objects.requireNonNull(clock);
    }
    @Transactional(readOnly = true)
    public RecordFeedbackContext assemble(UUID memberId, UUID recordId, long sourceRevision) {
        Objects.requireNonNull(memberId, "memberId"); Objects.requireNonNull(recordId, "recordId");
        var record = recordRepository.findByIdAndMember_Id(recordId, memberId);
        if (record == null || record.isDeleted()) throw new BusinessException(ErrorCode.FARMING_RECORD_NOT_FOUND);
        var detail = toDetail(record, recordId);
        int photoCount = mediaRepository.findByRecord_Id(recordId).size();
        var weather = fetchWeather(record);
        return new RecordFeedbackContext(RecordFeedbackContext.RECORD_FEEDBACK_CONTEXT_SCHEMA_VERSION,
            new RecordFeedbackMemberContext(record.getMember().getId() == null ? memberId : record.getMember().getId(), record.getMember().getExperienceLevel(), record.getMember().getManagementType()),
            new RecordFeedbackFarmContext(requireId(record.getFarm().getId(), ErrorCode.FARM_NOT_FOUND), record.getFarm().getName(), record.getFarm().getRoadAddress(), record.getFarm().getLatitude(), record.getFarm().getLongitude()),
            new RecordFeedbackCropContext(requireId(record.getCrop().getId(), ErrorCode.CROP_NOT_FOUND), record.getCrop().getName(), record.getCrop().getUsePartCategory()),
            new RecordFeedbackRecordContext(record.getId() == null ? recordId : record.getId(), sourceRevision, record.getWorkedAt(), record.getWorkType(), detail, record.getWeatherCondition(), record.getWeatherTemperature(), record.getMemo(), photoCount), weather.weather(), weather.warnings());
    }
    private UUID requireId(UUID id, ErrorCode error) { if (id == null) throw new BusinessException(error); return id; }
    private RecordFeedbackWorkDetail toDetail(FarmingRecord record, UUID recordId) {
        return switch (record.getWorkType()) {
            case PLANTING -> { var it = plantingRecordRepository.findByRecord_Id(recordId); if (it == null) throw new BusinessException(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED); yield new PlantingFeedbackDetail(it.getPlantingMethod(), it.getSeedAmount(), it.getSeedAmountUnit(), it.getSeedlingCount(), it.getSeedlingUnit(), it.getPropagationMethod()); }
            case WATERING -> { var it = wateringRecordRepository.findByRecord_Id(recordId); yield it == null ? new WateringFeedbackDetail(null, null) : new WateringFeedbackDetail(it.getIrrigationAmount(), it.getIrrigationMethod()); }
            case FERTILIZING -> { var it = fertilizingRecordRepository.findByRecord_Id(recordId); if (it == null) throw new BusinessException(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED); yield new FertilizingFeedbackDetail(it.getMaterialName(), it.getAmount(), it.getAmountUnit(), it.getApplicationMethod()); }
            case PEST_CONTROL -> { var it = pestControlRecordRepository.findByRecord_Id(recordId); if (it == null) throw new BusinessException(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED); yield new PestControlFeedbackDetail(it.getPesticide().getBrandName(), it.getPesticideAmount(), it.getPesticideAmountUnit(), it.getTotalSprayAmount(), it.getTotalSprayAmountUnit().name(), it.getPest() == null ? null : it.getPest().getName()); }
            case WEEDING -> { var it = weedingRecordRepository.findByRecord_Id(recordId); yield new WeedingFeedbackDetail(it == null ? null : it.getWeedingMethod()); }
            case HARVEST -> { var it = harvestRecordRepository.findByRecord_Id(recordId); if (it == null) throw new BusinessException(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED); yield new HarvestFeedbackDetail(it.getHarvestAmount(), it.getHarvestAmount() == null, it.getMedicinalPart(), it.getHarvestSource(), it.getGrowthPeriod(), "MONTH", it.isLastHarvest()); }
            case PRUNING, ETC -> CommonFeedbackDetail.INSTANCE;
        };
    }
    private WeatherResult fetchWeather(FarmingRecord record) {
        var today = LocalDate.now(clock);
        var workedOn = record.getWorkedAt().toLocalDate();
        if (workedOn.isBefore(today.minusDays(7)) || workedOn.isAfter(today)) return new WeatherResult(null, Collections.singletonList("weather_skipped_for_historical_record"));
        var latitude = record.getFarm().getLatitude(); var longitude = record.getFarm().getLongitude();
        if (latitude == null || longitude == null) return new WeatherResult(null, Collections.singletonList("weather_location_unavailable"));
        try { return new WeatherResult(weatherPort.fetch(latitude, longitude, 7), Collections.emptyList()); }
        catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.WEATHER_LOCATION_REQUIRED) return new WeatherResult(null, Collections.singletonList("weather_location_unavailable"));
            if (exception.getErrorCode() != ErrorCode.WEATHER_PROVIDER_UNAVAILABLE) throw exception;
            return new WeatherResult(fallbackWeather(), Collections.singletonList("weather_provider_unavailable"));
        } catch (RuntimeException exception) { return new WeatherResult(fallbackWeather(), Collections.singletonList("weather_provider_unavailable")); }
    }
    private RecordFeedbackLiveWeather fallbackWeather() { return new RecordFeedbackLiveWeather(new RecordFeedbackCurrentWeather(WeatherFallback.TEMPERATURE, WeatherFallback.INSTANCE.getCONDITION().getText(), LocalDateTime.now(clock)), Collections.emptyList(), "FALLBACK"); }
    private record WeatherResult(RecordFeedbackLiveWeather weather, List<String> warnings) {}
}
