package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordMedia;
import com.chamchamcham.domain.farming.FarmingRecordMediaRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.FertilizingMethod;
import com.chamchamcham.domain.farming.FertilizingRecord;
import com.chamchamcham.domain.farming.FertilizingRecordRepository;
import com.chamchamcham.domain.farming.HarvestRecord;
import com.chamchamcham.domain.farming.HarvestRecordRepository;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.PestControlRecord;
import com.chamchamcham.domain.farming.PestControlRecordRepository;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PlantingRecord;
import com.chamchamcham.domain.farming.PlantingRecordRepository;
import com.chamchamcham.domain.farming.PropagationMethod;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SeedlingUnit;
import com.chamchamcham.domain.farming.SprayAmountUnit;
import com.chamchamcham.domain.farming.WateringRecord;
import com.chamchamcham.domain.farming.WateringRecordRepository;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.chamchamcham.domain.farming.WeedingRecord;
import com.chamchamcham.domain.farming.WeedingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.ManagementType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.Pesticide;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

@ExtendWith({MockitoExtension.class})
class RecordFeedbackContextAssemblerTest {
    @Mock private FarmingRecordRepository recordRepository;
    @Mock private FarmingRecordMediaRepository mediaRepository;
    @Mock private PlantingRecordRepository plantingRecordRepository;
    @Mock private WateringRecordRepository wateringRecordRepository;
    @Mock private FertilizingRecordRepository fertilizingRecordRepository;
    @Mock private PestControlRecordRepository pestControlRecordRepository;
    @Mock private WeedingRecordRepository weedingRecordRepository;
    @Mock private HarvestRecordRepository harvestRecordRepository;
    @Mock private RecordFeedbackWeatherPort weatherPort;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneOffset.UTC);
    private RecordFeedbackContextAssembler assembler;
    private FarmingRecord record;

    @BeforeEach
    void setUp() throws Exception {
        assembler = new RecordFeedbackContextAssembler(recordRepository, mediaRepository, plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository, weatherPort, clock);
    }

    @Test
    @DisplayName("planting maps planting method and optional propagation fields")
    void planting_maps_planting_method_and_optional_propagation_fields() throws Exception {
        stubOwnedRecord(WorkType.PLANTING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(plantingRecordRepository.findByRecord_Id(recordId)).thenReturn(new PlantingRecord(null, record, PlantingMethod.SEEDLING, null, null, 18, SeedlingUnit.JU, PropagationMethod.CUTTING));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertBaseContext(context);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.PLANTING);
        assertThat(context.getRecord().getDetail()).isEqualTo(new PlantingFeedbackDetail(PlantingMethod.SEEDLING, null, null, 18, SeedlingUnit.JU, PropagationMethod.CUTTING));
        verify(plantingRecordRepository).findByRecord_Id(recordId);
        verifyOnlyPlantingRepositoryUsed();
    }

    @Test
    @DisplayName("watering maps nullable amount and method and missing row becomes empty detail")
    void watering_maps_nullable_amount_and_method_and_missing_row_becomes_empty_detail() throws Exception {
        stubOwnedRecord(WorkType.WATERING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(wateringRecordRepository.findByRecord_Id(recordId)).thenReturn(new WateringRecord(null, record, IrrigationAmount.NORMAL, IrrigationMethod.DRIP));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.WATERING);
        assertThat(context.getRecord().getDetail()).isEqualTo(new WateringFeedbackDetail(IrrigationAmount.NORMAL, IrrigationMethod.DRIP));
        verify(wateringRecordRepository).findByRecord_Id(recordId);
        verifyOnlyWateringRepositoryUsed();
    }

    @Test
    @DisplayName("watering missing row returns empty detail")
    void watering_missing_row_returns_empty_detail() throws Exception {
        stubOwnedRecord(WorkType.WATERING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(wateringRecordRepository.findByRecord_Id(recordId)).thenReturn(null);
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getDetail()).isEqualTo(new WateringFeedbackDetail(null, null));
        verify(wateringRecordRepository).findByRecord_Id(recordId);
        verifyOnlyWateringRepositoryUsed();
    }

    @Test
    @DisplayName("fertilizing maps actual material amount unit and method without statistics")
    void fertilizing_maps_actual_material_amount_unit_and_method_without_statistics() throws Exception {
        stubOwnedRecord(WorkType.FERTILIZING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(fertilizingRecordRepository.findByRecord_Id(recordId)).thenReturn(new FertilizingRecord(null, record, "유기질비료", new BigDecimal("2500.0000"), FertilizerAmountUnit.G, FertilizingMethod.SOIL));
        stubSharedContext(2);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.FERTILIZING);
        assertThat(context.getRecord().getDetail()).isEqualTo(new FertilizingFeedbackDetail("유기질비료", new BigDecimal("2500.0000"), FertilizerAmountUnit.G, FertilizingMethod.SOIL));
        assertThat(context.getRecord().getPhotoCount()).isEqualTo(2);
        assertThat(Arrays.stream(RecordFeedbackContext.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList()).doesNotContain("recentRecords", "workTypeStats", "cropCycle");
        verify(fertilizingRecordRepository).findByRecord_Id(recordId);
        verify(mediaRepository).findByRecord_Id(recordId);
        verifyOnlyFertilizingRepositoryUsed();
    }

    @Test
    @DisplayName("pest control maps pesticide amounts spray amount and pest")
    void pest_control_maps_pesticide_amounts_spray_amount_and_pest() throws Exception {
        stubOwnedRecord(WorkType.PEST_CONTROL, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(pestControlRecordRepository.findByRecord_Id(recordId)).thenReturn(new PestControlRecord(null, record, new Pesticide(UUID.fromString("00000000-0000-0000-0000-000000000010"), "만코제브 수화제", "가가방"), new BigDecimal("120.0000"), PesticideAmountUnit.ML, new BigDecimal("20.0000"), SprayAmountUnit.ML, new Pest(UUID.fromString("00000000-0000-0000-0000-000000000011"), "점무늬병")));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.PEST_CONTROL);
        assertThat(context.getRecord().getDetail()).isEqualTo(new PestControlFeedbackDetail("가가방", new BigDecimal("120.0000"), PesticideAmountUnit.ML, new BigDecimal("20.0000"), "ML", "점무늬병"));
        verify(pestControlRecordRepository).findByRecord_Id(recordId);
        verifyOnlyPestControlRepositoryUsed();
    }

    @Test
    @DisplayName("weeding maps nullable method and missing row becomes empty detail")
    void weeding_maps_nullable_method_and_missing_row_becomes_empty_detail() throws Exception {
        stubOwnedRecord(WorkType.WEEDING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(weedingRecordRepository.findByRecord_Id(recordId)).thenReturn(new WeedingRecord(null, record, WeedingMethod.HAND));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.WEEDING);
        assertThat(context.getRecord().getDetail()).isEqualTo(new WeedingFeedbackDetail(WeedingMethod.HAND));
        verify(weedingRecordRepository).findByRecord_Id(recordId);
        verifyOnlyWeedingRepositoryUsed();
    }

    @Test
    @DisplayName("weeding missing row returns empty detail")
    void weeding_missing_row_returns_empty_detail() throws Exception {
        stubOwnedRecord(WorkType.WEEDING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(weedingRecordRepository.findByRecord_Id(recordId)).thenReturn(null);
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getDetail()).isEqualTo(new WeedingFeedbackDetail(null));
        verify(weedingRecordRepository).findByRecord_Id(recordId);
        verifyOnlyWeedingRepositoryUsed();
    }

    @Test
    @DisplayName("pruning uses common detail and no detail repository call")
    void pruning_uses_common_detail_and_no_detail_repository_call() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.PRUNING);
        assertThat(context.getRecord().getDetail()).isEqualTo(CommonFeedbackDetail.INSTANCE);
        verifyNoDetailRepositoryInteractions();
    }

    @Test
    @DisplayName("harvest maps amount part source growth period and final flag")
    void harvest_maps_amount_part_source_growth_period_and_final_flag() throws Exception {
        stubOwnedRecord(WorkType.HARVEST, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(harvestRecordRepository.findByRecord_Id(recordId)).thenReturn(new HarvestRecord(null, record, new BigDecimal("4.2000"), CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 18, true));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.HARVEST);
        assertThat(context.getRecord().getDetail()).isEqualTo(new HarvestFeedbackDetail(new BigDecimal("4.2000"), false, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 18, "MONTH", true));
        verify(harvestRecordRepository).findByRecord_Id(recordId);
        verifyOnlyHarvestRepositoryUsed();
    }

    @Test
    @DisplayName("harvest null amount marks amount unknown while keeping required growth period")
    void harvest_null_amount_marks_amount_unknown_while_keeping_required_growth_period() throws Exception {
        stubOwnedRecord(WorkType.HARVEST, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(harvestRecordRepository.findByRecord_Id(recordId)).thenReturn(new HarvestRecord(null, record, null, null, HarvestSource.FORAGED, 12, false));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getDetail()).isEqualTo(new HarvestFeedbackDetail(null, true, null, HarvestSource.FORAGED, 12, "MONTH", false));
        verifyOnlyHarvestRepositoryUsed();
    }

    @Test
    @DisplayName("etc uses common detail and memo only without inferred category")
    void etc_uses_common_detail_and_memo_only_without_inferred_category() throws Exception {
        stubOwnedRecord(WorkType.ETC, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        stubSharedContext(0);
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.ETC);
        assertThat(context.getRecord().getMemo()).isEqualTo("오전 작업 기록");
        assertThat(context.getRecord().getDetail()).isEqualTo(CommonFeedbackDetail.INSTANCE);
        verifyNoDetailRepositoryInteractions();
    }

    @Test
    @DisplayName("another member record returns farming record not found")
    void another_member_record_returns_farming_record_not_found() throws Exception {
        when(recordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(null);
        assertThatThrownBy(() -> assembler.assemble(memberId, recordId, SOURCE_REVISION)).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.FARMING_RECORD_NOT_FOUND));
        verifyNoDetailRepositoryInteractions();
        verifyNoInteractions(mediaRepository, weatherPort);
    }

    @Test
    @DisplayName("soft deleted record returns farming record not found")
    void soft_deleted_record_returns_farming_record_not_found() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        record.softDelete();
        assertThatThrownBy(() -> assembler.assemble(memberId, recordId, SOURCE_REVISION)).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.FARMING_RECORD_NOT_FOUND));
    }

    @Test
    @DisplayName("missing required detail row remains data error")
    void missing_required_detail_row_remains_data_error() throws Exception {
        stubOwnedRecord(WorkType.FERTILIZING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(fertilizingRecordRepository.findByRecord_Id(recordId)).thenReturn(null);
        assertThatThrownBy(() -> assembler.assemble(memberId, recordId, SOURCE_REVISION)).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED));
        verify(fertilizingRecordRepository).findByRecord_Id(recordId);
        verifyNoInteractions(mediaRepository, weatherPort);
    }

    @Test
    @DisplayName("missing planting detail row remains data error")
    void missing_planting_detail_row_remains_data_error() throws Exception {
        stubOwnedRecord(WorkType.PLANTING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(plantingRecordRepository.findByRecord_Id(recordId)).thenReturn(null);
        assertThatThrownBy(() -> assembler.assemble(memberId, recordId, SOURCE_REVISION)).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED));
        verify(plantingRecordRepository).findByRecord_Id(recordId);
        verifyNoInteractions(mediaRepository, weatherPort);
    }

    @Test
    @DisplayName("missing pest control detail row remains data error")
    void missing_pest_control_detail_row_remains_data_error() throws Exception {
        stubOwnedRecord(WorkType.PEST_CONTROL, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(pestControlRecordRepository.findByRecord_Id(recordId)).thenReturn(null);
        assertThatThrownBy(() -> assembler.assemble(memberId, recordId, SOURCE_REVISION)).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED));
        verify(pestControlRecordRepository).findByRecord_Id(recordId);
        verifyNoInteractions(mediaRepository, weatherPort);
    }

    @Test
    @DisplayName("missing harvest detail row remains data error")
    void missing_harvest_detail_row_remains_data_error() throws Exception {
        stubOwnedRecord(WorkType.HARVEST, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(harvestRecordRepository.findByRecord_Id(recordId)).thenReturn(null);
        assertThatThrownBy(() -> assembler.assemble(memberId, recordId, SOURCE_REVISION)).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED));
        verify(harvestRecordRepository).findByRecord_Id(recordId);
        verifyNoInteractions(mediaRepository, weatherPort);
    }

    @Test
    @DisplayName("no coordinate weather produces warning without fetching provider")
    void no_coordinate_weather_produces_warning_without_fetching_provider() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, null, null, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(mediaRepository.findByRecord_Id(recordId)).thenReturn(List.of());
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getWeather()).isNull();
        assertThat(context.getWarnings()).containsExactly("weather_location_unavailable");
        verifyNoInteractions(weatherPort);
    }

    @Test
    @DisplayName("provider business failure falls back to fixed weather with warning")
    void provider_business_failure_falls_back_to_fixed_weather_with_warning() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(mediaRepository.findByRecord_Id(recordId)).thenReturn(List.of());
        when(weatherPort.fetch(37.1, 128.2, 7)).thenThrow(new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getWeather()).isNotNull();
        assertThat(context.getWeather().getSource()).isEqualTo("FALLBACK");
        assertThat(context.getWeather().getCurrent().getSkyCondition()).isEqualTo("흐림");
        assertThat(context.getWeather().getCurrent().getTemperatureC()).isEqualTo(20);
        assertThat(context.getWeather().getForecastDays()).isEmpty();
        assertThat(context.getWarnings()).containsExactly("weather_provider_unavailable");
    }

    @Test
    @DisplayName("provider runtime failure falls back to fixed weather with warning")
    void provider_runtime_failure_falls_back_to_fixed_weather_with_warning() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 8, 30));
        when(mediaRepository.findByRecord_Id(recordId)).thenReturn(List.of());
        when(weatherPort.fetch(37.1, 128.2, 7)).thenThrow(new IllegalStateException("provider down"));
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getWeather()).isNotNull();
        assertThat(context.getWeather().getSource()).isEqualTo("FALLBACK");
        assertThat(context.getWeather().getCurrent().getSkyCondition()).isEqualTo("흐림");
        assertThat(context.getWarnings()).containsExactly("weather_provider_unavailable");
    }

    @Test
    @DisplayName("record from exactly seven days ago still fetches live weather")
    void record_from_exactly_seven_days_ago_still_fetches_live_weather() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, 37.1, 128.2, LocalDateTime.of(2026, 7, 1, 9, 0));
        when(mediaRepository.findByRecord_Id(recordId)).thenReturn(List.of());
        when(weatherPort.fetch(37.1, 128.2, 7)).thenReturn(liveWeather());
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getWeather()).isEqualTo(liveWeather());
        assertThat(context.getWarnings()).isEmpty();
        verify(weatherPort).fetch(37.1, 128.2, 7);
    }

    @Test
    @DisplayName("record older than seven days skips live weather")
    void record_older_than_seven_days_skips_live_weather() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, 37.1, 128.2, LocalDateTime.of(2026, 6, 30, 9, 0));
        when(mediaRepository.findByRecord_Id(recordId)).thenReturn(List.of());
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getWeather()).isNull();
        assertThat(context.getWarnings()).containsExactly("weather_skipped_for_historical_record");
        verifyNoInteractions(weatherPort);
    }

    @Test
    @DisplayName("future record skips live weather")
    void future_record_skips_live_weather() throws Exception {
        stubOwnedRecord(WorkType.PRUNING, 37.1, 128.2, LocalDateTime.of(2026, 7, 9, 9, 0));
        when(mediaRepository.findByRecord_Id(recordId)).thenReturn(List.of());
        var context = assembler.assemble(memberId, recordId, SOURCE_REVISION);
        assertThat(context.getWeather()).isNull();
        assertThat(context.getWarnings()).containsExactly("weather_skipped_for_historical_record");
        verifyNoInteractions(weatherPort);
    }
    private void stubOwnedRecord(WorkType workType, Double latitude, Double longitude, LocalDateTime workedAt) {
        var member = also(new Member(memberId, "farmer@example.test", null), migratedMember -> {
        migratedMember.setExperienceLevel(3);
        migratedMember.setManagementType(ManagementType.NON_REGISTERED_FARMER);
        });
        var farm = also(new Farm(farmId, member, "청년약초밭", "강원특별자치도 평창군"), migratedFarm -> {
        migratedFarm.setLatitude(latitude);
        migratedFarm.setLongitude(longitude);
        });
        var crop = new Crop(cropId, 1001, "참당귀", CropUsePartCategory.ROOT_BARK);
        record = new FarmingRecord(recordId, member, farm, crop, workType, workedAt, "맑음", 27, "오전 작업 기록", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
        when(recordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(record);
    }
    private void stubSharedContext(int photoCount) {
        when(mediaRepository.findByRecord_Id(recordId)).thenReturn(java.util.stream.IntStream.range(0, photoCount).mapToObj(it -> mock(FarmingRecordMedia.class)).toList());
        when(weatherPort.fetch(37.1, 128.2, 7)).thenReturn(liveWeather());
    }
    private void assertBaseContext(RecordFeedbackContext context) {
        assertThat(context.getSchemaVersion()).isEqualTo("record-feedback-context.v2");
        assertThat(context.getMember().getMemberId()).isEqualTo(memberId);
        assertThat(context.getMember().getExperienceLevel()).isEqualTo(3);
        assertThat(context.getMember().getManagementType()).isEqualTo(ManagementType.NON_REGISTERED_FARMER);
        assertThat(context.getFarm().getFarmId()).isEqualTo(farmId);
        assertThat(context.getFarm().getName()).isEqualTo("청년약초밭");
        assertThat(context.getFarm().getRoadAddress()).isEqualTo("강원특별자치도 평창군");
        assertThat(context.getFarm().getLatitude()).isEqualTo(37.1);
        assertThat(context.getFarm().getLongitude()).isEqualTo(128.2);
        assertThat(context.getCrop().getCropId()).isEqualTo(cropId);
        assertThat(context.getCrop().getName()).isEqualTo("참당귀");
        assertThat(context.getCrop().getUsePartCategory()).isEqualTo(CropUsePartCategory.ROOT_BARK);
        assertThat(context.getRecord().getRecordId()).isEqualTo(recordId);
        assertThat(context.getRecord().getSourceRevision()).isEqualTo(SOURCE_REVISION);
        assertThat(context.getRecord().getWorkedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 8, 30));
        assertThat(context.getRecord().getRecordedWeatherCondition()).isEqualTo("맑음");
        assertThat(context.getRecord().getRecordedTemperatureC()).isEqualTo(27);
        assertThat(context.getRecord().getMemo()).isEqualTo("오전 작업 기록");
        assertThat(context.getRecord().getPhotoCount()).isEqualTo(0);
        assertThat(context.getWeather()).isEqualTo(liveWeather());
        assertThat(context.getWarnings()).isEmpty();
    }
    private RecordFeedbackLiveWeather liveWeather() {
        return new RecordFeedbackLiveWeather(new RecordFeedbackCurrentWeather(25, "구름많음", LocalDateTime.of(2026, 7, 1, 9, 0)), java.util.Arrays.asList(new RecordFeedbackForecastDay(LocalDate.of(2026, 7, 2), new BigDecimal("12.5"), 60, new BigDecimal("29.5"), new BigDecimal("20.0"), new BigDecimal("81.0"), new BigDecimal("3.5"), java.util.Arrays.asList("RAIN"))), "fake");
    }
    private static final long SOURCE_REVISION = 4L;
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static <T> T also(T value, java.util.function.Consumer<T> action) { action.accept(value); return value; }

    private void verifyOnlyPlantingRepositoryUsed() { verifyNoInteractions(wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository); }
    private void verifyOnlyWateringRepositoryUsed() { verifyNoInteractions(plantingRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository); }
    private void verifyOnlyFertilizingRepositoryUsed() { verifyNoInteractions(plantingRecordRepository, wateringRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository); }
    private void verifyOnlyPestControlRepositoryUsed() { verifyNoInteractions(plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, weedingRecordRepository, harvestRecordRepository); }
    private void verifyOnlyWeedingRepositoryUsed() { verifyNoInteractions(plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, harvestRecordRepository); }
    private void verifyOnlyHarvestRepositoryUsed() { verifyNoInteractions(plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository); }
    private void verifyNoDetailRepositoryInteractions() { verifyNoInteractions(plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository); }
}
