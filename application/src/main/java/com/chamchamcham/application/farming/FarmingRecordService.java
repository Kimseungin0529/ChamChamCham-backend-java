package com.chamchamcham.application.farming;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.RecordFeedbackLifecycleService;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.report.FarmingCycleReportProjectionService;
import com.chamchamcham.application.report.ReportScope;
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
import com.chamchamcham.domain.farming.FertilizingRecord;
import com.chamchamcham.domain.farming.FertilizingRecordRepository;
import com.chamchamcham.domain.farming.HarvestRecord;
import com.chamchamcham.domain.farming.HarvestRecordRepository;
import com.chamchamcham.domain.farming.PestControlRecord;
import com.chamchamcham.domain.farming.PestControlRecordRepository;
import com.chamchamcham.domain.farming.PlantingRecord;
import com.chamchamcham.domain.farming.PlantingRecordRepository;
import com.chamchamcham.domain.farming.WateringRecord;
import com.chamchamcham.domain.farming.WateringRecordRepository;
import com.chamchamcham.domain.farming.WeedingRecord;
import com.chamchamcham.domain.farming.WeedingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.PestRepository;
import com.chamchamcham.domain.pesticide.Pesticide;
import com.chamchamcham.domain.pesticide.PesticideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
@Transactional
public class FarmingRecordService {
    private static final int MEMO_PREVIEW_LENGTH = 80;
    private static final int COUNT_QUERY_SIZE = 1;
    private final MemberRepository memberRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final MemberCropRepository memberCropRepository;
    private final FarmingRecordRepository farmingRecordRepository;
    private final FarmingRecordMediaRepository farmingRecordMediaRepository;
    private final FarmingRecordQueryRepository farmingRecordQueryRepository;
    private final UploadedMediaRepository uploadedMediaRepository;
    private final PlantingRecordRepository plantingRecordRepository;
    private final WateringRecordRepository wateringRecordRepository;
    private final FertilizingRecordRepository fertilizingRecordRepository;
    private final PestControlRecordRepository pestControlRecordRepository;
    private final WeedingRecordRepository weedingRecordRepository;
    private final HarvestRecordRepository harvestRecordRepository;
    private final PesticideRepository pesticideRepository;
    private final PestRepository pestRepository;
    private final FarmingRecordDetailValidator detailValidator;
    private final OpaqueCursorCodec cursorCodec;
    private final FarmingCycleReportProjectionService reportProjectionService;
    private final RecordFeedbackLifecycleService recordFeedbackLifecycleService;

    public FarmingRecordService(MemberRepository memberRepository, FarmRepository farmRepository, CropRepository cropRepository,
        MemberCropRepository memberCropRepository, FarmingRecordRepository farmingRecordRepository,
        FarmingRecordMediaRepository farmingRecordMediaRepository, FarmingRecordQueryRepository farmingRecordQueryRepository,
        UploadedMediaRepository uploadedMediaRepository, PlantingRecordRepository plantingRecordRepository,
        WateringRecordRepository wateringRecordRepository, FertilizingRecordRepository fertilizingRecordRepository,
        PestControlRecordRepository pestControlRecordRepository, WeedingRecordRepository weedingRecordRepository,
        HarvestRecordRepository harvestRecordRepository, PesticideRepository pesticideRepository, PestRepository pestRepository,
        FarmingRecordDetailValidator detailValidator, OpaqueCursorCodec cursorCodec, FarmingCycleReportProjectionService reportProjectionService,
        RecordFeedbackLifecycleService recordFeedbackLifecycleService) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.farmingRecordRepository = Objects.requireNonNull(farmingRecordRepository, "farmingRecordRepository");
        this.farmingRecordMediaRepository = Objects.requireNonNull(farmingRecordMediaRepository, "farmingRecordMediaRepository");
        this.farmingRecordQueryRepository = Objects.requireNonNull(farmingRecordQueryRepository, "farmingRecordQueryRepository");
        this.uploadedMediaRepository = Objects.requireNonNull(uploadedMediaRepository, "uploadedMediaRepository");
        this.plantingRecordRepository = Objects.requireNonNull(plantingRecordRepository, "plantingRecordRepository");
        this.wateringRecordRepository = Objects.requireNonNull(wateringRecordRepository, "wateringRecordRepository");
        this.fertilizingRecordRepository = Objects.requireNonNull(fertilizingRecordRepository, "fertilizingRecordRepository");
        this.pestControlRecordRepository = Objects.requireNonNull(pestControlRecordRepository, "pestControlRecordRepository");
        this.weedingRecordRepository = Objects.requireNonNull(weedingRecordRepository, "weedingRecordRepository");
        this.harvestRecordRepository = Objects.requireNonNull(harvestRecordRepository, "harvestRecordRepository");
        this.pesticideRepository = Objects.requireNonNull(pesticideRepository, "pesticideRepository");
        this.pestRepository = Objects.requireNonNull(pestRepository, "pestRepository");
        this.detailValidator = Objects.requireNonNull(detailValidator, "detailValidator");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
        this.reportProjectionService = Objects.requireNonNull(reportProjectionService, "reportProjectionService");
        this.recordFeedbackLifecycleService = Objects.requireNonNull(recordFeedbackLifecycleService, "recordFeedbackLifecycleService");
    }

    public FarmingRecordResult.RecordId create(FarmingRecordCommand.Create command) {
        Objects.requireNonNull(command, "command");
        detailValidator.validate(command);
        var member = findMember(command.getMemberId());
        var farm = findFarm(command.getFarmId(), command.getMemberId());
        var crop = findCrop(command.getCropId());
        assertCropRegisteredToFarm(command.getMemberId(), command.getFarmId(), command.getCropId());
        var media = validateMedia(command.getMemberId(), command.getMediaIds());
        var record = farmingRecordRepository.save(new FarmingRecord(null, member, farm, crop, command.getWorkType(),
            command.getWorkedAt(), command.getWeatherCondition(), command.getWeatherTemperature(), command.getMemo(), command.getEntryMode(), false));
        saveDetail(record, command);
        attachMedia(record, media);
        reportProjectionService.rebuild(new ReportScope(command.getMemberId(), command.getFarmId(), command.getCropId()));
        recordFeedbackLifecycleService.enqueue(record);
        return new FarmingRecordResult.RecordId(required(record.getId(), "Required value was null."), record.getWorkType());
    }

    @Transactional(readOnly = true)
    public FarmingRecordResult.Page search(FarmingRecordSearchCondition condition) throws JsonProcessingException {
        Objects.requireNonNull(condition, "condition");
        validatePageSize(condition.getSize());
        var cursor = decodeCursor(condition.getCursor());
        var result = farmingRecordQueryRepository.search(toQueryCondition(condition, cursor, condition.getSize() + 1));
        var visibleRows = result.rows().subList(0, Math.min(result.rows().size(), condition.getSize()));
        var nextCursor = result.rows().size() > condition.getSize() && !visibleRows.isEmpty() ? encodeCursor(visibleRows.getLast()) : null;
        return new FarmingRecordResult.Page(visibleRows.stream().map(this::toSummary).collect(Collectors.toCollection(ArrayList::new)), nextCursor);
    }

    @Transactional(readOnly = true)
    public long count(FarmingRecordSearchCondition condition) {
        Objects.requireNonNull(condition, "condition");
        return farmingRecordQueryRepository.count(toQueryCondition(condition, null, COUNT_QUERY_SIZE));
    }

    private FarmingRecordQueryRepository.SearchCondition toQueryCondition(FarmingRecordSearchCondition condition,
            FarmingRecordQueryRepository.Cursor cursor, int size) {
        var keyword = condition.getKeyword() == null ? null : trim(condition.getKeyword());
        if (keyword != null && keyword.isEmpty()) keyword = null;
        var trimmedKeyword = keyword;
        return new FarmingRecordQueryRepository.SearchCondition(condition.getMemberId(), condition.getCropIds(), condition.getWorkTypes(),
            condition.getStartDate() == null ? null : condition.getStartDate().atStartOfDay(),
            condition.getEndDate() == null ? null : condition.getEndDate().plusDays(1).atStartOfDay(), keyword,
            keyword == null ? List.of() : Arrays.stream(WorkType.values()).filter(t -> t.getLabel().contains(trimmedKeyword)).collect(Collectors.toCollection(ArrayList::new)),

            keyword == null ? List.of() : Arrays.stream(CropUsePartCategory.values()).filter(t -> t.getLabel().contains(trimmedKeyword)).collect(Collectors.toCollection(ArrayList::new)),
                cursor, size);
    }

    @Transactional(readOnly = true)
    public FarmingRecordResult.Detail getDetail(UUID memberId, UUID recordId) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(recordId, "recordId");
        var record = findRecord(recordId);
        assertOwner(record, memberId);
        return toDetail(record);
    }

    public FarmingRecordResult.RecordId update(FarmingRecordCommand.Update command) {
        Objects.requireNonNull(command, "command");
        var record = findRecord(command.getRecordId());
        assertOwner(record, command.getMemberId());
        detailValidator.validate(command);
        var farm = findFarm(command.getFarmId(), command.getMemberId());
        var crop = findCrop(command.getCropId());
        assertCropRegisteredToFarm(command.getMemberId(), command.getFarmId(), command.getCropId());
        var existingRecordMedia = farmingRecordMediaRepository.findByRecord_Id(command.getRecordId());
        var media = validateUpdatedMedia(record, command.getMemberId(), command.getMediaIds());
        var previousScope = new ReportScope(command.getMemberId(), required(record.getFarm().getId(), "Persisted farm id is required"),
            required(record.getCrop().getId(), "Persisted crop id is required"));
        var previousWorkType = record.getWorkType();
        record.update(farm, crop, command.getWorkType(), command.getWorkedAt(), command.getWeatherCondition(), command.getWeatherTemperature(), command.getMemo());
        deleteExistingDetail(record, previousWorkType);
        farmingRecordRepository.flush();
        saveDetail(record, command);
        syncMedia(record, existingRecordMedia, media);
        reportProjectionService.rebuildAll(List.of(previousScope, new ReportScope(command.getMemberId(), command.getFarmId(), command.getCropId())));
        recordFeedbackLifecycleService.enqueue(record);
        return new FarmingRecordResult.RecordId(required(record.getId(), "Required value was null."), record.getWorkType());
    }

    public void delete(FarmingRecordCommand.Delete command) {
        Objects.requireNonNull(command, "command");
        var record = findRecord(command.getRecordId());
        assertOwner(record, command.getMemberId());
        var scope = new ReportScope(command.getMemberId(), required(record.getFarm().getId(), "Persisted farm id is required"),
            required(record.getCrop().getId(), "Persisted crop id is required"));
        record.softDelete();
        reportProjectionService.rebuild(scope);
        recordFeedbackLifecycleService.staleFor(command.getRecordId());
    }

    private void saveDetail(FarmingRecord record, FarmingRecordDetailPayload payload) {
        switch (payload.getWorkType()) {
            case PLANTING -> {
                var detail = requiredDetail(payload.getPlanting());
                plantingRecordRepository.save(new PlantingRecord(null, record, detail.getPlantingMethod(), detail.getSeedAmount(),
                    detail.getSeedAmountUnit(), detail.getSeedlingCount(), detail.getSeedlingUnit(), detail.getPropagationMethod()));
            }
            case WATERING -> {
                var detail = payload.getWatering();
                if (detail != null) wateringRecordRepository.save(new WateringRecord(null, record, detail.getIrrigationAmount(), detail.getIrrigationMethod()));
            }
            case FERTILIZING -> {
                var detail = requiredDetail(payload.getFertilizing());
                fertilizingRecordRepository.save(new FertilizingRecord(null, record, detail.getMaterialName(), detail.getAmount(), detail.getAmountUnit(),
                    detail.getApplicationMethod()));
            }
            case PEST_CONTROL -> {
                var detail = requiredDetail(payload.getPestControl());
                pestControlRecordRepository.save(new PestControlRecord(null, record, findPesticide(detail.getPesticideId()),
                    detail.getPesticideAmount(), detail.getPesticideAmountUnit(), detail.getTotalSprayAmount(), detail.getTotalSprayAmountUnit(),
                    detail.getPestId() == null ? null : findPest(detail.getPestId())));
            }
            case WEEDING -> {
                var detail = payload.getWeeding();
                if (detail != null) weedingRecordRepository.save(new WeedingRecord(null, record, detail.getWeedingMethod()));
            }
            case HARVEST -> {
                var detail = requiredDetail(payload.getHarvest());
                harvestRecordRepository.save(new HarvestRecord(null, record, detail.getAmountUnknown() ? null : detail.getHarvestAmount(),
                    detail.getMedicinalPart(), detail.getHarvestSource(), detail.getGrowthPeriod(), detail.isLastHarvest()));
            }
            case PRUNING, ETC -> {}
        }
    }

    private void deleteExistingDetail(FarmingRecord record, WorkType workType) {
        switch (workType) {
            case PLANTING -> plantingRecordRepository.deleteByRecord(record);
            case WATERING -> wateringRecordRepository.deleteByRecord(record);
            case FERTILIZING -> fertilizingRecordRepository.deleteByRecord(record);
            case PEST_CONTROL -> pestControlRecordRepository.deleteByRecord(record);
            case WEEDING -> weedingRecordRepository.deleteByRecord(record);
            case HARVEST -> harvestRecordRepository.deleteByRecord(record);
            case PRUNING, ETC -> {}
        }
    }

    private FarmingRecordResult.Detail toDetail(FarmingRecord record) {
        var recordId = required(record.getId(), "Persisted farming record id is required");
        var images = farmingRecordMediaRepository.findByRecord_Id(recordId).stream()
            .sorted(Comparator.comparingInt(FarmingRecordMedia::getDisplayOrder))
            .map(it -> new FarmingRecordResult.MediaItem(required(it.getUploadedMedia().getId(), "Persisted uploaded media id is required"),
                it.getUploadedMedia().getFileUrl())).collect(Collectors.toCollection(ArrayList::new));
        FarmingRecordResult.PlantingDetail planting = null;
        FarmingRecordResult.WateringDetail watering = null;
        FarmingRecordResult.FertilizingDetail fertilizing = null;
        FarmingRecordResult.PestControlDetail pestControl = null;
        FarmingRecordResult.WeedingDetail weeding = null;
        FarmingRecordResult.HarvestDetail harvest = null;
        switch (record.getWorkType()) {
            case PLANTING -> {
                var it = plantingRecordRepository.findByRecord_Id(recordId);
                if (it != null) planting = new FarmingRecordResult.PlantingDetail(it.getPlantingMethod(), it.getSeedAmount(), it.getSeedAmountUnit(),
                    it.getSeedlingCount(), it.getSeedlingUnit(), it.getPropagationMethod());
            }
            case WATERING -> {
                var it = wateringRecordRepository.findByRecord_Id(recordId);
                if (it != null) watering = new FarmingRecordResult.WateringDetail(it.getIrrigationAmount(), it.getIrrigationMethod());
            }
            case FERTILIZING -> {
                var it = fertilizingRecordRepository.findByRecord_Id(recordId);
                if (it != null) fertilizing = new FarmingRecordResult.FertilizingDetail(it.getMaterialName(), it.getAmount(), it.getAmountUnit(),
                    it.getApplicationMethod());
            }
            case PEST_CONTROL -> {
                var it = pestControlRecordRepository.findByRecord_Id(recordId);
                if (it != null) pestControl = new FarmingRecordResult.PestControlDetail(required(it.getPesticide().getId(), "Persisted pesticide id is required"),
                    it.getPesticide().getItemName(), it.getPesticideAmount(), it.getPesticideAmountUnit(), it.getTotalSprayAmount(), it.getTotalSprayAmountUnit(),
                    it.getPest() == null ? null : it.getPest().getId(), it.getPest() == null ? null : it.getPest().getName());
            }
            case WEEDING -> {
                var it = weedingRecordRepository.findByRecord_Id(recordId);
                if (it != null) weeding = new FarmingRecordResult.WeedingDetail(it.getWeedingMethod());
            }
            case HARVEST -> {
                var it = harvestRecordRepository.findByRecord_Id(recordId);
                if (it != null) harvest = new FarmingRecordResult.HarvestDetail(it.getHarvestAmount(), it.getMedicinalPart(), it.getHarvestSource(),
                    it.getGrowthPeriod(), it.isLastHarvest());
            }
            case PRUNING, ETC -> {}
        }
        return new FarmingRecordResult.Detail(recordId, required(record.getFarm().getId(), "Persisted farm id is required"), record.getFarm().getName(),
            required(record.getCrop().getId(), "Persisted crop id is required"), record.getCrop().getName(), record.getWorkType(), record.getWorkedAt(),
            record.getWeatherCondition(), record.getWeatherTemperature(), record.getMemo(), planting, watering, fertilizing, pestControl, weeding, harvest,
            images, record.getCreatedAt(), record.getUpdatedAt());
    }

    private FarmingRecordResult.Summary toSummary(FarmingRecordQueryRepository.Row row) {
        var record = row.record();
        return new FarmingRecordResult.Summary(required(record.getId(), "Persisted farming record id is required"),
            required(record.getCrop().getId(), "Persisted crop id is required"), record.getCrop().getName(), record.getWorkType(), record.getWorkedAt(),
            record.getWeatherCondition(), record.getWeatherTemperature(), record.getMemo().substring(0, Math.min(record.getMemo().length(), MEMO_PREVIEW_LENGTH)),
            row.thumbnailUrl(), row.irrigationMethod(), row.harvestAmount(), row.pesticideName(), row.weedingMethod(), row.plantingMethod(), row.materialName());
    }

    private List<UploadedMedia> validateMedia(UUID memberId, List<UUID> mediaIds) {
        validateDistinctMediaIds(mediaIds);
        if (mediaIds.isEmpty()) return List.of();
        var mediaById = new LinkedHashMap<UUID, UploadedMedia>();
        for (var media : uploadedMediaRepository.findAllById(mediaIds)) mediaById.put(required(media.getId(), "Persisted media id is required"), media);
        return mediaIds.stream().map(mediaId -> {
            var media = mediaById.get(mediaId);
            if (media == null) throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
            if (!Objects.equals(media.getOwner().getId(), memberId)) throw new BusinessException(ErrorCode.MEDIA_NOT_OWNED);
            if (media.getUsageType() != UploadedMediaUsageType.FARMING_RECORD) throw new BusinessException(ErrorCode.MEDIA_USAGE_MISMATCH);
            if (!media.isAttachable()) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            return media;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    // 현재 기록에 연결된 ATTACHED 미디어만 수정 시 재첨부할 수 있다.
    private List<UploadedMedia> validateUpdatedMedia(FarmingRecord record, UUID memberId, List<UUID> mediaIds) {
        validateDistinctMediaIds(mediaIds);
        if (mediaIds.isEmpty()) return List.of();
        var recordId = required(record.getId(), "Persisted farming record id is required");
        var mediaById = new LinkedHashMap<UUID, UploadedMedia>();
        for (var media : uploadedMediaRepository.findAllById(mediaIds)) mediaById.put(required(media.getId(), "Persisted media id is required"), media);
        var recordMediaByMediaId = new LinkedHashMap<UUID, FarmingRecordMedia>();
        for (var item : farmingRecordMediaRepository.findByUploadedMediaIdIn(mediaIds)) {
            recordMediaByMediaId.put(required(item.getUploadedMedia().getId(), "Persisted media id is required"), item);
        }
        return mediaIds.stream().map(mediaId -> {
            var media = mediaById.get(mediaId);
            if (media == null) throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
            if (!Objects.equals(media.getOwner().getId(), memberId)) throw new BusinessException(ErrorCode.MEDIA_NOT_OWNED);
            if (media.getUsageType() != UploadedMediaUsageType.FARMING_RECORD) throw new BusinessException(ErrorCode.MEDIA_USAGE_MISMATCH);
            var recordMedia = recordMediaByMediaId.get(mediaId);
            if (recordMedia != null && !Objects.equals(recordMedia.getRecord().getId(), recordId)) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            if (!media.isAttachable() && media.getStatus() != UploadedMediaStatus.ATTACHED) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            if (media.getStatus() == UploadedMediaStatus.ATTACHED && (recordMedia == null || !Objects.equals(recordMedia.getRecord().getId(), recordId))) {
                throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            }
            return media;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private void validateDistinctMediaIds(List<UUID> mediaIds) {
        if (mediaIds.size() != new HashSet<>(mediaIds).size()) throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private void attachMedia(FarmingRecord record, List<UploadedMedia> media) {
        if (media.isEmpty()) return;
        media.forEach(UploadedMedia::markAttached);
        var items = new ArrayList<FarmingRecordMedia>();
        for (int i = 0; i < media.size(); i++) items.add(new FarmingRecordMedia(null, record, media.get(i), i));
        farmingRecordMediaRepository.saveAll(items);
    }

    // 삭제를 먼저 flush하여 같은 uploaded_media_id 재삽입 시 고유 제약 충돌을 막는다.
    private void syncMedia(FarmingRecord record, List<FarmingRecordMedia> existingRecordMedia, List<UploadedMedia> media) {
        var finalMediaIds = media.stream().map(it -> required(it.getId(), "Persisted media id is required")).collect(Collectors.toSet());
        existingRecordMedia.stream().filter(it -> !finalMediaIds.contains(required(it.getUploadedMedia().getId(), "Persisted media id is required")))
            .forEach(it -> it.getUploadedMedia().markDeleted());
        farmingRecordMediaRepository.deleteByRecord(record);
        farmingRecordMediaRepository.flush();
        attachMedia(record, media);
    }

    private void validatePageSize(int size) {
        if (size <= 0 || size == Integer.MAX_VALUE) throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private FarmingRecordQueryRepository.Cursor decodeCursor(String cursor) {
        if (cursor == null || trim(cursor).isEmpty()) return null;
        var payload = cursorCodec.decode(cursor, FarmingRecordCursorPayload.class);
        return new FarmingRecordQueryRepository.Cursor(payload.getWorkedAt(), payload.getId());
    }

    private String encodeCursor(FarmingRecordQueryRepository.Row row) throws JsonProcessingException {
        return cursorCodec.encode(new FarmingRecordCursorPayload(row.record().getWorkedAt(), required(row.record().getId(), "Persisted farming record id is required")));
    }

    private FarmingRecord findRecord(UUID recordId) {
        var record = farmingRecordRepository.findByIdAndIsDeletedFalse(recordId);
        if (record == null) throw new BusinessException(ErrorCode.FARMING_RECORD_NOT_FOUND);
        return record;
    }

    private void assertOwner(FarmingRecord record, UUID memberId) {
        if (!Objects.equals(record.getMember().getId(), memberId)) throw new BusinessException(ErrorCode.FARMING_RECORD_FORBIDDEN);
    }

    private Member findMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Farm findFarm(UUID farmId, UUID memberId) {
        var farm = farmRepository.findByIdAndOwnerId(farmId, memberId);
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        return farm;
    }

    private Crop findCrop(UUID cropId) {
        return cropRepository.findById(cropId).orElseThrow(() -> new BusinessException(ErrorCode.CROP_NOT_FOUND));
    }

    private void assertCropRegisteredToFarm(UUID memberId, UUID farmId, UUID cropId) {
        if (!memberCropRepository.existsByMemberIdAndFarmIdAndCropId(memberId, farmId, cropId)) throw new BusinessException(ErrorCode.FARMING_RECORD_CROP_NOT_IN_FARM);
    }

    private Pesticide findPesticide(UUID pesticideId) {
        return pesticideRepository.findById(pesticideId).orElseThrow(() -> new BusinessException(ErrorCode.PESTICIDE_NOT_FOUND));
    }

    private Pest findPest(UUID pestId) {
        return pestRepository.findById(pestId).orElseThrow(() -> new BusinessException(ErrorCode.PEST_NOT_FOUND));
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }

    private static <T> T requiredDetail(T value) {
        if (value == null) throw new BusinessException(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED);
        return value;
    }

    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && (Character.isWhitespace(value.charAt(start)) || Character.isSpaceChar(value.charAt(start)))) start++;
        while (end > start && (Character.isWhitespace(value.charAt(end - 1)) || Character.isSpaceChar(value.charAt(end - 1)))) end--;
        return value.substring(start, end);
    }
}
