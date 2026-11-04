package com.chamchamcham.application.farming;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.RecordFeedbackLifecycleService;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.report.FarmingCycleReportProjectionService;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordMedia;
import com.chamchamcham.domain.farming.FarmingRecordMediaRepository;
import com.chamchamcham.domain.farming.FarmingRecordQueryRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.FertilizingRecordRepository;
import com.chamchamcham.domain.farming.HarvestRecord;
import com.chamchamcham.domain.farming.HarvestRecordRepository;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.PestControlRecord;
import com.chamchamcham.domain.farming.PestControlRecordRepository;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PlantingRecord;
import com.chamchamcham.domain.farming.PlantingRecordRepository;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SprayAmountUnit;
import com.chamchamcham.domain.farming.WateringRecordRepository;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.chamchamcham.domain.farming.WeedingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.PestRepository;
import com.chamchamcham.domain.pesticide.Pesticide;
import com.chamchamcham.domain.pesticide.PesticideRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class FarmingRecordServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID otherMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID secondRecordId = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private final UUID mediaId1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private final UUID replacementMediaId = UUID.fromString("00000000-0000-0000-0000-000000000402");
    private final OpaqueCursorCodec cursorCodec = new OpaqueCursorCodec();
    @Mock private MemberRepository memberRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private CropRepository cropRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private FarmingRecordRepository farmingRecordRepository;
    @Mock private FarmingRecordMediaRepository farmingRecordMediaRepository;
    @Mock private FarmingRecordQueryRepository farmingRecordQueryRepository;
    @Mock private UploadedMediaRepository uploadedMediaRepository;
    @Mock private PlantingRecordRepository plantingRecordRepository;
    @Mock private WateringRecordRepository wateringRecordRepository;
    @Mock private FertilizingRecordRepository fertilizingRecordRepository;
    @Mock private PestControlRecordRepository pestControlRecordRepository;
    @Mock private WeedingRecordRepository weedingRecordRepository;
    @Mock private HarvestRecordRepository harvestRecordRepository;
    @Mock private PesticideRepository pesticideRepository;
    @Mock private PestRepository pestRepository;
    @Mock private FarmingRecordDetailValidator detailValidator;
    @Mock private FarmingCycleReportProjectionService reportProjectionService;
    @Mock private RecordFeedbackLifecycleService recordFeedbackLifecycleService;
    private FarmingRecordService service;
    private Member member;
    private Member otherMember;
    private Farm farm;
    private Crop crop;
    private UploadedMedia media1;
    private UploadedMedia replacementMedia;

    @BeforeEach
    void setUp() throws Exception {
        service = new FarmingRecordService(memberRepository, farmRepository, cropRepository, memberCropRepository, farmingRecordRepository, farmingRecordMediaRepository, farmingRecordQueryRepository, uploadedMediaRepository, plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository, pesticideRepository, pestRepository, detailValidator, cursorCodec, reportProjectionService, recordFeedbackLifecycleService);
        lenient().when(memberCropRepository.existsByMemberIdAndFarmIdAndCropId(memberId, farmId, cropId)).thenReturn(true);
        member = new Member(memberId, "" + memberId + "@example.com", null);
        otherMember = new Member(otherMemberId, "" + otherMemberId + "@example.com", null);
        farm = new Farm(farmId, member, "약초농장", "서울시 강남구");
        crop = new Crop(cropId, cropId.hashCode(), "황기", CropUsePartCategory.ROOT_BARK);
        media1 = uploadedMedia(mediaId1, UploadedMediaUsageType.FARMING_RECORD, UploadedMediaStatus.TEMP);
        replacementMedia = uploadedMedia(replacementMediaId, UploadedMediaUsageType.FARMING_RECORD, UploadedMediaStatus.TEMP);
    }
    private FarmingRecordCommand.Create baseCommand(WorkType workType, FarmingRecordCommand.PlantingDetail planting, FarmingRecordCommand.HarvestDetail harvest, FarmingRecordCommand.PestControlDetail pestControl, List<UUID> mediaIds) {
        return new FarmingRecordCommand.Create(memberId, farmId, cropId, workType, LocalDateTime.of(2026, 6, 1, 9, 0), "맑음", 20, "memo", planting, null, null, pestControl, null, harvest, mediaIds, EntryMode.MANUAL);
    }
    private FarmingRecordCommand.Update updateCommand(WorkType workType, FarmingRecordCommand.PlantingDetail planting, FarmingRecordCommand.HarvestDetail harvest, List<UUID> mediaIds) {
        return new FarmingRecordCommand.Update(memberId, recordId, farmId, cropId, workType, LocalDateTime.of(2026, 6, 1, 9, 0), "맑음", 20, "memo", planting, null, null, null, null, harvest, mediaIds);
    }
    private FarmingRecord existingRecord(UUID id, Member owner, WorkType workType, LocalDateTime workedAt) {
        return new FarmingRecord(id, owner, farm, crop, workType, workedAt, "맑음", 20, "memo", EntryMode.MANUAL, false);
    }
    private UploadedMedia uploadedMedia(UUID id, UploadedMediaUsageType usageType, UploadedMediaStatus status) {
        var media = new UploadedMedia(id, member, UploadedMediaType.IMAGE, usageType, "https://example.test/" + id + ".jpg", "farming/" + id);
        media.setStatus(status);
        return media;
    }
    private void stubFarmingRecordSave() {
        when(farmingRecordRepository.save(any(FarmingRecord.class))).thenAnswer(invocation -> {
            FarmingRecord toSave = invocation.getArgument(0);
            return new FarmingRecord(UUID.randomUUID(), toSave.getMember(), toSave.getFarm(), toSave.getCrop(), toSave.getWorkType(), toSave.getWorkedAt(), toSave.getWeatherCondition(), toSave.getWeatherTemperature(), toSave.getMemo(), toSave.getEntryMode(), false);
        });
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<FarmingRecordMedia> capturedFarmingRecordMedia() {
        ArgumentCaptor<Iterable<FarmingRecordMedia>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(farmingRecordMediaRepository).saveAll(captor.capture());
        return java.util.stream.StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }
    private void setTimestamps(BaseTimeEntity entity, LocalDateTime dateTime) {
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "createdAt", dateTime);
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "updatedAt", dateTime);
    }

    @Test
    @DisplayName("create saves planting detail when workType is PLANTING")
    void create_saves_planting_detail_when_workType_is_PLANTING() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.TEN, SeedAmountUnit.G, null, null, null), null, null, List.of());
        var result = service.create(command);
        assertEquals(WorkType.PLANTING, result.getWorkType());
        var captor = ArgumentCaptor.forClass(PlantingRecord.class);
        verify(plantingRecordRepository).save(captor.capture());
        assertEquals(BigDecimal.TEN, captor.getValue().getSeedAmount());
        assertEquals(SeedAmountUnit.G, captor.getValue().getSeedAmountUnit());
        verifyNoInteractions(harvestRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository);
    }

    @Test
    @DisplayName("create saves pest control detail when workType is PEST_CONTROL")
    void create_saves_pest_control_detail_when_workType_is_PEST_CONTROL() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        var pesticideId = UUID.randomUUID();
        var pestId = UUID.randomUUID();
        var pesticide = new Pesticide(pesticideId, "만코제브 수화제", "가가방");
        var pest = new Pest(pestId, "역병");
        when(pesticideRepository.findById(pesticideId)).thenReturn(Optional.of(pesticide));
        when(pestRepository.findById(pestId)).thenReturn(Optional.of(pest));
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.PEST_CONTROL, null, null, new FarmingRecordCommand.PestControlDetail(pesticideId, BigDecimal.ONE, PesticideAmountUnit.ML, BigDecimal.TEN, SprayAmountUnit.ML, pestId), List.of());
        service.create(command);
        var captor = ArgumentCaptor.forClass(PestControlRecord.class);
        verify(pestControlRecordRepository).save(captor.capture());
        assertEquals(pesticideId, captor.getValue().getPesticide().getId());
        assertEquals(pestId, captor.getValue().getPest().getId());
        verifyNoInteractions(harvestRecordRepository, wateringRecordRepository, fertilizingRecordRepository, plantingRecordRepository, weedingRecordRepository);
    }

    @Test
    @DisplayName("create throws when pesticide id does not exist")
    void create_throws_when_pesticide_id_does_not_exist() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        var pesticideId = UUID.randomUUID();
        when(pesticideRepository.findById(pesticideId)).thenReturn(Optional.empty());
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.PEST_CONTROL, null, null, new FarmingRecordCommand.PestControlDetail(pesticideId, BigDecimal.ONE, PesticideAmountUnit.ML, BigDecimal.TEN, SprayAmountUnit.ML, null), List.of());
        var exception = assertThrows(BusinessException.class, () -> service.create(command));
        assertEquals(ErrorCode.PESTICIDE_NOT_FOUND, exception.getErrorCode());
        verify(pestControlRecordRepository, never()).save(any(PestControlRecord.class));
    }

    @Test
    @DisplayName("create persists weather fields")
    void create_persists_weather_fields() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        stubFarmingRecordSave();
        service.create(baseCommand(WorkType.PRUNING, null, null, null, List.of()));
        var captor = ArgumentCaptor.forClass(FarmingRecord.class);
        verify(farmingRecordRepository).save(captor.capture());
        assertEquals("맑음", captor.getValue().getWeatherCondition());
        assertEquals(20, captor.getValue().getWeatherTemperature());
    }

    @Test
    @DisplayName("create attaches up to five media")
    void create_attaches_up_to_five_media() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(media1));
        stubFarmingRecordSave();
        service.create(baseCommand(WorkType.PRUNING, null, null, null, java.util.Arrays.asList(mediaId1)));
        assertEquals(UploadedMediaStatus.ATTACHED, media1.getStatus());
        assertThat(capturedFarmingRecordMedia().stream().map(it -> it.getUploadedMedia().getId()).toList()).containsExactly(mediaId1);
    }

    @Test
    @DisplayName("create saves harvest detail when workType is HARVEST")
    void create_saves_harvest_detail_when_workType_is_HARVEST() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.HARVEST, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, false, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false), null, List.of());
        service.create(command);
        var captor = ArgumentCaptor.forClass(HarvestRecord.class);
        verify(harvestRecordRepository).save(captor.capture());
        assertEquals(BigDecimal.TEN, captor.getValue().getHarvestAmount());
        assertEquals(CropUsePartCategory.ROOT_BARK, captor.getValue().getMedicinalPart());
    }

    @Test
    @DisplayName("create stores null harvest amount when amount is unknown")
    void create_stores_null_harvest_amount_when_amount_is_unknown() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.HARVEST, null, new FarmingRecordCommand.HarvestDetail(null, true, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false), null, List.of());
        service.create(command);
        var captor = ArgumentCaptor.forClass(HarvestRecord.class);
        verify(harvestRecordRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getHarvestAmount());
    }

    @Test
    @DisplayName("create saves no detail row when workType is PRUNING")
    void create_saves_no_detail_row_when_workType_is_PRUNING() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.PRUNING, null, null, null, List.of());
        var result = service.create(command);
        assertEquals(WorkType.PRUNING, result.getWorkType());
        verifyNoInteractions(plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository);
    }

    @Test
    @DisplayName("create saves no detail row when workType is ETC")
    void create_saves_no_detail_row_when_workType_is_ETC() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.ETC, null, null, null, List.of());
        var result = service.create(command);
        assertEquals(WorkType.ETC, result.getWorkType());
        verifyNoInteractions(plantingRecordRepository, wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository, harvestRecordRepository);
    }

    @Test
    @DisplayName("create rejects fertilizing without required detail and saves nothing")
    void create_rejects_fertilizing_without_required_detail_and_saves_nothing() throws Exception {
        var command = baseCommand(WorkType.FERTILIZING, null, null, null, List.of());
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED)).when(detailValidator).validate(command);
        var exception = assertThrows(BusinessException.class, () -> service.create(command));
        assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        verify(farmingRecordRepository, never()).save(any(FarmingRecord.class));
    }

    @Test
    @DisplayName("create throws when member not found")
    void create_throws_when_member_not_found() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());
        var command = baseCommand(WorkType.PRUNING, null, null, null, List.of());
        var exception = assertThrows(BusinessException.class, () -> service.create(command));
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("create throws when farm is not owned by member")
    void create_throws_when_farm_is_not_owned_by_member() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(null);
        var command = baseCommand(WorkType.PRUNING, null, null, null, List.of());
        var exception = assertThrows(BusinessException.class, () -> service.create(command));
        assertEquals(ErrorCode.FARM_NOT_FOUND, exception.getErrorCode());
        verify(farmingRecordRepository, never()).save(any(FarmingRecord.class));
    }

    @Test
    @DisplayName("create throws BusinessException not IllegalArgumentException when detail is missing despite validator passing")
    void create_throws_BusinessException_not_IllegalArgumentException_when_detail_is_missing_despite_validator_passing() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        stubFarmingRecordSave();
        var command = baseCommand(WorkType.FERTILIZING, null, null, null, List.of());
        var exception = assertThrows(BusinessException.class, () -> service.create(command));
        assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("update replaces detail row when workType changes")
    void update_replaces_detail_row_when_workType_changes() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PLANTING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        var result = service.update(updateCommand(WorkType.HARVEST, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, false, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false), List.of()));
        assertEquals(WorkType.HARVEST, result.getWorkType());
        assertEquals(WorkType.HARVEST, record.getWorkType());
        verify(plantingRecordRepository).deleteByRecord(record);
        var captor = ArgumentCaptor.forClass(HarvestRecord.class);
        verify(harvestRecordRepository).save(captor.capture());
        assertEquals(BigDecimal.TEN, captor.getValue().getHarvestAmount());
        verifyNoInteractions(wateringRecordRepository, fertilizingRecordRepository, pestControlRecordRepository, weedingRecordRepository);
    }

    @Test
    @DisplayName("update replaces detail row when workType is unchanged")
    void update_replaces_detail_row_when_workType_is_unchanged() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PLANTING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        service.update(updateCommand(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.ONE, SeedAmountUnit.G, null, null, null), null, List.of()));
        verify(plantingRecordRepository).deleteByRecord(record);
        var captor = ArgumentCaptor.forClass(PlantingRecord.class);
        verify(plantingRecordRepository).save(captor.capture());
        assertEquals(BigDecimal.ONE, captor.getValue().getSeedAmount());
    }

    @Test
    @DisplayName("update replaces media associations")
    void update_replaces_media_associations() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(replacementMediaId))).thenReturn(java.util.Arrays.asList(replacementMedia));
        service.update(updateCommand(WorkType.PRUNING, null, null, java.util.Arrays.asList(replacementMediaId)));
        verify(farmingRecordMediaRepository).deleteByRecord(record);
        verify(farmingRecordMediaRepository).flush();
        assertEquals(UploadedMediaStatus.ATTACHED, replacementMedia.getStatus());
    }

    @Test
    @DisplayName("update flushes the media delete before attaching new rows")
    void update_flushes_the_media_delete_before_attaching_new_rows() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(replacementMediaId))).thenReturn(java.util.Arrays.asList(replacementMedia));
        service.update(updateCommand(WorkType.PRUNING, null, null, java.util.Arrays.asList(replacementMediaId)));
        var inOrder = org.mockito.Mockito.inOrder(farmingRecordMediaRepository);
        inOrder.verify(farmingRecordMediaRepository).deleteByRecord(record);
        inOrder.verify(farmingRecordMediaRepository).flush();
        inOrder.verify(farmingRecordMediaRepository).saveAll(org.mockito.ArgumentMatchers.anyIterable());
    }

    @Test
    @DisplayName("update rejects duplicate media ids in the same request")
    void update_rejects_duplicate_media_ids_in_the_same_request() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        var exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(WorkType.PRUNING, null, null, java.util.Arrays.asList(mediaId1, mediaId1))));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verify(farmingRecordMediaRepository, never()).deleteByRecord(record);
    }

    @Test
    @DisplayName("create rejects duplicate media ids in the same request")
    void create_rejects_duplicate_media_ids_in_the_same_request() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        var exception = assertThrows(BusinessException.class, () -> service.create(baseCommand(WorkType.PRUNING, null, null, null, java.util.Arrays.asList(mediaId1, mediaId1))));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verify(farmingRecordRepository, never()).save(any(FarmingRecord.class));
    }

    @Test
    @DisplayName("update keeps an already-attached photo that belongs to this same record")
    void update_keeps_an_already_attached_photo_that_belongs_to_this_same_record() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        var alreadyAttached = uploadedMedia(mediaId1, UploadedMediaUsageType.FARMING_RECORD, UploadedMediaStatus.ATTACHED);
        var ownRecordMedia = new FarmingRecordMedia(null, record, alreadyAttached, 0);
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(alreadyAttached));
        when(farmingRecordMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(ownRecordMedia));
        service.update(updateCommand(WorkType.PRUNING, null, null, java.util.Arrays.asList(mediaId1)));
        verify(farmingRecordMediaRepository).deleteByRecord(record);
        assertEquals(UploadedMediaStatus.ATTACHED, alreadyAttached.getStatus());
    }

    @Test
    @DisplayName("update rejects a photo already attached to a different record")
    void update_rejects_a_photo_already_attached_to_a_different_record() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        var otherRecord = existingRecord(secondRecordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        var attachedElsewhere = uploadedMedia(mediaId1, UploadedMediaUsageType.FARMING_RECORD, UploadedMediaStatus.ATTACHED);
        var otherRecordMedia = new FarmingRecordMedia(null, otherRecord, attachedElsewhere, 0);
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(attachedElsewhere));
        when(farmingRecordMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(otherRecordMedia));
        var exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(WorkType.PRUNING, null, null, java.util.Arrays.asList(mediaId1))));
        assertEquals(ErrorCode.MEDIA_NOT_ATTACHABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("update marks a dropped photo as deleted while keeping the retained one")
    void update_marks_a_dropped_photo_as_deleted_while_keeping_the_retained_one() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        var kept = media1;
        var dropped = replacementMedia;
        var keptRecordMedia = new FarmingRecordMedia(null, record, kept, 0);
        var droppedRecordMedia = new FarmingRecordMedia(null, record, dropped, 1);
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(farmingRecordMediaRepository.findByRecord_Id(recordId)).thenReturn(java.util.Arrays.asList(keptRecordMedia, droppedRecordMedia));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(kept));
        when(farmingRecordMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(keptRecordMedia));
        service.update(updateCommand(WorkType.PRUNING, null, null, java.util.Arrays.asList(mediaId1)));
        assertEquals(UploadedMediaStatus.DELETED, dropped.getStatus());
        assertEquals(UploadedMediaStatus.ATTACHED, kept.getStatus());
    }

    @Test
    @DisplayName("update throws not found for missing or deleted record")
    void update_throws_not_found_for_missing_or_deleted_record() throws Exception {
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(WorkType.PRUNING, null, null, List.of())));
        assertEquals(ErrorCode.FARMING_RECORD_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("update throws forbidden for record owned by another member")
    void update_throws_forbidden_for_record_owned_by_another_member() throws Exception {
        var record = existingRecord(recordId, otherMember, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        var exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(WorkType.PRUNING, null, null, List.of())));
        assertEquals(ErrorCode.FARMING_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("create throws when crop is not registered to the farm")
    void create_throws_when_crop_is_not_registered_to_the_farm() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(memberCropRepository.existsByMemberIdAndFarmIdAndCropId(memberId, farmId, cropId)).thenReturn(false);
        var exception = assertThrows(BusinessException.class, () -> service.create(baseCommand(WorkType.WATERING, null, null, null, List.of())));
        assertEquals(ErrorCode.FARMING_RECORD_CROP_NOT_IN_FARM, exception.getErrorCode());
        verify(farmingRecordRepository, never()).save(any(FarmingRecord.class));
    }

    @Test
    @DisplayName("update throws when crop is not registered to the farm")
    void update_throws_when_crop_is_not_registered_to_the_farm() throws Exception {
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0)));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(memberCropRepository.existsByMemberIdAndFarmIdAndCropId(memberId, farmId, cropId)).thenReturn(false);
        var exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(WorkType.WATERING, null, null, List.of())));
        assertEquals(ErrorCode.FARMING_RECORD_CROP_NOT_IN_FARM, exception.getErrorCode());
    }

    @Test
    @DisplayName("delete sets isDeleted without removing the row")
    void delete_sets_isDeleted_without_removing_the_row() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        service.delete(new FarmingRecordCommand.Delete(memberId, recordId));
        assertTrue(record.isDeleted());
        verify(farmingRecordRepository, never()).delete(any(FarmingRecord.class));
    }

    @Test
    @DisplayName("delete throws not found for missing or deleted record")
    void delete_throws_not_found_for_missing_or_deleted_record() throws Exception {
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> service.delete(new FarmingRecordCommand.Delete(memberId, recordId)));
        assertEquals(ErrorCode.FARMING_RECORD_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("delete throws forbidden for record owned by another member")
    void delete_throws_forbidden_for_record_owned_by_another_member() throws Exception {
        var record = existingRecord(recordId, otherMember, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        var exception = assertThrows(BusinessException.class, () -> service.delete(new FarmingRecordCommand.Delete(memberId, recordId)));
        assertEquals(ErrorCode.FARMING_RECORD_FORBIDDEN, exception.getErrorCode());
        assertFalse(record.isDeleted());
    }

    @Test
    @DisplayName("getDetail returns full detail with images and type specific detail")
    void getDetail_returns_full_detail_with_images_and_type_specific_detail() throws Exception {
        var record = existingRecord(recordId, member, WorkType.HARVEST, LocalDateTime.of(2026, 6, 1, 9, 0));
        setTimestamps(record, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(harvestRecordRepository.findByRecord_Id(recordId)).thenReturn(new HarvestRecord(null, record, BigDecimal.TEN, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, true));
        when(farmingRecordMediaRepository.findByRecord_Id(recordId)).thenReturn(java.util.Arrays.asList(new FarmingRecordMedia(null, record, media1, 0)));
        var detail = service.getDetail(memberId, recordId);
        assertEquals(recordId, detail.getId());
        assertEquals(cropId, detail.getCropId());
        assertEquals(farmId, detail.getFarmId());
        assertEquals(WorkType.HARVEST, detail.getWorkType());
        assertEquals("맑음", detail.getWeatherCondition());
        assertEquals(20, detail.getWeatherTemperature());
        assertEquals(BigDecimal.TEN, detail.getHarvest().getHarvestAmount());
        assertEquals(true, detail.getHarvest().isLastHarvest());
        assertThat(detail.getImages()).containsExactly(new FarmingRecordResult.MediaItem(mediaId1, media1.getFileUrl()));
    }

    @Test
    @DisplayName("getDetail exposes selected pesticide item name instead of brand name")
    void getDetail_exposes_selected_pesticide_item_name_instead_of_brand_name() throws Exception {
        var record = existingRecord(recordId, member, WorkType.PEST_CONTROL, LocalDateTime.of(2026, 6, 1, 9, 0));
        setTimestamps(record, LocalDateTime.of(2026, 6, 1, 9, 0));
        var pesticideId = UUID.randomUUID();
        var pesticide = new Pesticide(pesticideId, "만코제브 수화제", "빔");
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        when(farmingRecordMediaRepository.findByRecord_Id(recordId)).thenReturn(List.of());
        when(pestControlRecordRepository.findByRecord_Id(recordId)).thenReturn(new PestControlRecord(null, record, pesticide, BigDecimal.ONE, PesticideAmountUnit.ML, BigDecimal.TEN, SprayAmountUnit.ML, null));
        var detail = service.getDetail(memberId, recordId);
        assertEquals("만코제브 수화제", detail.getPestControl().getPesticideName());
    }

    @Test
    @DisplayName("getDetail throws not found for missing or deleted record")
    void getDetail_throws_not_found_for_missing_or_deleted_record() throws Exception {
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> service.getDetail(memberId, recordId));
        assertEquals(ErrorCode.FARMING_RECORD_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getDetail throws forbidden for record owned by another member")
    void getDetail_throws_forbidden_for_record_owned_by_another_member() throws Exception {
        var record = existingRecord(recordId, otherMember, WorkType.PRUNING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordRepository.findByIdAndIsDeletedFalse(recordId)).thenReturn(record);
        var exception = assertThrows(BusinessException.class, () -> service.getDetail(memberId, recordId));
        assertEquals(ErrorCode.FARMING_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("search maps application condition to query repository and returns next cursor")
    void search_maps_application_condition_to_query_repository_and_returns_next_cursor() throws Exception {
        var requestedSize = 1;
        var newestRecord = existingRecord(recordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 10, 0));
        var overflowRecord = existingRecord(secondRecordId, member, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0));
        when(farmingRecordQueryRepository.search(new FarmingRecordQueryRepository.SearchCondition(memberId, java.util.Arrays.asList(cropId), java.util.Arrays.asList(WorkType.PRUNING), null, null, null, List.of(), List.of(), null, requestedSize+ 1))).thenReturn(new FarmingRecordQueryRepository.SearchResult(java.util.Arrays.asList(new FarmingRecordQueryRepository.Row(newestRecord, "https://example.test/1.jpg", null, null, null, null, null, null), new FarmingRecordQueryRepository.Row(overflowRecord, null, null, null, null, null, null, null))));
        var page = service.search(new FarmingRecordSearchCondition(memberId, java.util.Arrays.asList(cropId), java.util.Arrays.asList(WorkType.PRUNING), null, null, null, null, requestedSize));
        assertThat(page.getItems()).hasSize(1);
        assertEquals(recordId, page.getItems().get(0).getId());
        assertThat(page.getNextCursor()).isNotBlank();
        var nextCursor = cursorCodec.decode(page.getNextCursor(), FarmingRecordCursorPayload.class);
        assertEquals(newestRecord.getWorkedAt(), nextCursor.getWorkedAt());
        assertEquals(recordId, nextCursor.getId());
    }

    @Test
    @DisplayName("search maps work type summary fields from query repository row")
    void search_maps_work_type_summary_fields_from_query_repository_row() throws Exception {
        var record = existingRecord(recordId, member, WorkType.WATERING, LocalDateTime.of(2026, 6, 1, 9, 0));
        when(farmingRecordQueryRepository.search(new FarmingRecordQueryRepository.SearchCondition(memberId, List.of(), List.of(), null, null, null, List.of(), List.of(), null, 21))).thenReturn(new FarmingRecordQueryRepository.SearchResult(java.util.Arrays.asList(new FarmingRecordQueryRepository.Row(record, null, IrrigationMethod.DRIP, BigDecimal.TEN, "친환경약제", WeedingMethod.HAND, PlantingMethod.SEEDLING, "유박비료"))));
        var page = service.search(new FarmingRecordSearchCondition(memberId, List.of(), List.of(), null, null, null, null, 20));
        var summary = page.getItems().get(0);
        assertEquals(IrrigationMethod.DRIP, summary.getIrrigationMethod());
        assertEquals(0, BigDecimal.TEN.compareTo(summary.getHarvestAmount()));
        assertEquals("친환경약제", summary.getPesticideName());
        assertEquals(WeedingMethod.HAND, summary.getWeedingMethod());
        assertEquals(PlantingMethod.SEEDLING, summary.getPlantingMethod());
        assertEquals("유박비료", summary.getMaterialName());
    }

    @Test
    @DisplayName("search converts start and end date to datetime bounds")
    void search_converts_start_and_end_date_to_datetime_bounds() throws Exception {
        when(farmingRecordQueryRepository.search(new FarmingRecordQueryRepository.SearchCondition(memberId, List.of(), List.of(), LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0), null, List.of(), List.of(), null, 21))).thenReturn(new FarmingRecordQueryRepository.SearchResult(List.of()));
        var page = service.search(new FarmingRecordSearchCondition(memberId, List.of(), List.of(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null, null, 20));
        assertThat(page.getItems()).isEmpty();
    }

    @Test
    @DisplayName("search rejects zero size")
    void search_rejects_zero_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(List.of(), List.of(), null, null, null, null, 0)));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(farmingRecordQueryRepository);
    }

    @Test
    @DisplayName("search rejects negative size")
    void search_rejects_negative_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(List.of(), List.of(), null, null, null, null, -1)));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(farmingRecordQueryRepository);
    }

    @Test
    @DisplayName("search rejects max integer size")
    void search_rejects_max_integer_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(List.of(), List.of(), null, null, null, null, Integer.MAX_VALUE)));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(farmingRecordQueryRepository);
    }

    @Test
    @DisplayName("search rejects malformed cursor")
    void search_rejects_malformed_cursor() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(List.of(), List.of(), null, null, null, "not-a-valid-cursor", 20)));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.getErrorCode());
        verifyNoInteractions(farmingRecordQueryRepository);
    }

    @Test
    @DisplayName("search resolves keyword into matched work type label")
    void search_resolves_keyword_into_matched_work_type_label() throws Exception {
        when(farmingRecordQueryRepository.search(new FarmingRecordQueryRepository.SearchCondition(memberId, List.of(), List.of(), null, null, "수확", java.util.Arrays.asList(WorkType.HARVEST), List.of(), null, 21))).thenReturn(new FarmingRecordQueryRepository.SearchResult(List.of()));
        var page = service.search(searchCondition(List.of(), List.of(), null, null, "수확", null, 20));
        assertThat(page.getItems()).isEmpty();
    }

    @Test
    @DisplayName("search resolves keyword into matched harvest part label")
    void search_resolves_keyword_into_matched_harvest_part_label() throws Exception {
        when(farmingRecordQueryRepository.search(new FarmingRecordQueryRepository.SearchCondition(memberId, List.of(), List.of(), null, null, "잎", List.of(), java.util.Arrays.asList(CropUsePartCategory.LEAF), null, 21))).thenReturn(new FarmingRecordQueryRepository.SearchResult(List.of()));
        var page = service.search(searchCondition(List.of(), List.of(), null, null, "잎", null, 20));
        assertThat(page.getItems()).isEmpty();
    }

    @Test
    @DisplayName("search passes blank keyword as null with no matched labels")
    void search_passes_blank_keyword_as_null_with_no_matched_labels() throws Exception {
        when(farmingRecordQueryRepository.search(new FarmingRecordQueryRepository.SearchCondition(memberId, List.of(), List.of(), null, null, null, List.of(), List.of(), null, 21))).thenReturn(new FarmingRecordQueryRepository.SearchResult(List.of()));
        var page = service.search(searchCondition(List.of(), List.of(), null, null, "   ", null, 20));
        assertThat(page.getItems()).isEmpty();
    }

    @Test
    @DisplayName("count delegates to query repository with derived matched labels and no cursor")
    void count_delegates_to_query_repository_with_derived_matched_labels_and_no_cursor() throws Exception {
        when(farmingRecordQueryRepository.count(new FarmingRecordQueryRepository.SearchCondition(memberId, java.util.Arrays.asList(cropId), List.of(), null, null, "수확", java.util.Arrays.asList(WorkType.HARVEST), List.of(), null, 1))).thenReturn(7L);
        var total = service.count(searchCondition(java.util.Arrays.asList(cropId), List.of(), null, null, "수확", null, 20));
        assertEquals(7L, total);
        verifyNoInteractions(farmingRecordRepository);
    }

    @Test
    @DisplayName("count passes blank keyword as null with no matched labels")
    void count_passes_blank_keyword_as_null_with_no_matched_labels() throws Exception {
        when(farmingRecordQueryRepository.count(new FarmingRecordQueryRepository.SearchCondition(memberId, List.of(), List.of(), null, null, null, List.of(), List.of(), null, 1))).thenReturn(0L);
        var total = service.count(searchCondition(List.of(), List.of(), null, null, "   ", null, 20));
        assertEquals(0L, total);
    }
    private FarmingRecordSearchCondition searchCondition(List<UUID> cropIds, List<WorkType> workTypes, LocalDate startDate, LocalDate endDate, String keyword, String cursor, int size) {
        return new FarmingRecordSearchCondition(memberId, cropIds, workTypes, startDate, endDate, keyword, cursor, size);
    }
}
