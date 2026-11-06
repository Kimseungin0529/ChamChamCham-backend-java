package com.chamchamcham.api.farming;

import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.ApiApplication;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.application.report.FarmingCycleReportProjectionService;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordMediaRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.WateringRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@SpringBootTest(
    classes = {ApiApplication.class},
    properties = {
        "spring.datasource.url=jdbc:h2:mem:farming-record-media-sync;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
}
)
@ActiveProfiles("test")
class FarmingRecordMediaSyncIntegrationTest {
    @Autowired private FarmingRecordService farmingRecordService;
    @Autowired private FarmingRecordRepository farmingRecordRepository;
    @Autowired private WateringRecordRepository wateringRecordRepository;
    @Autowired private FarmingRecordMediaRepository farmingRecordMediaRepository;
    @Autowired private FarmingCycleReportRepository reportRepository;
    @Autowired private UploadedMediaRepository uploadedMediaRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberCropRepository memberCropRepository;
    @Autowired private FarmRepository farmRepository;
    @Autowired private CropRepository cropRepository;
    @Autowired private RecordFeedbackRepository recordFeedbackRepository;
    @MockBean
    private FarmingCycleReportProjectionService projectionService;
    private Member member;
    private Farm farm;
    private Crop crop;
    private UUID memberId() { return java.util.Objects.requireNonNull(member.getId()); }
    private UUID farmId() { return java.util.Objects.requireNonNull(farm.getId()); }
    private UUID cropId() { return java.util.Objects.requireNonNull(crop.getId()); }
    @BeforeEach
    void setUp() throws Exception {
        recordFeedbackRepository.deleteAll();
        reportRepository.deleteAllInBatch();
        farmingRecordMediaRepository.deleteAllInBatch();
        wateringRecordRepository.deleteAllInBatch();
        farmingRecordRepository.deleteAllInBatch();
        uploadedMediaRepository.deleteAllInBatch();
        memberCropRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();
        cropRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        member = memberRepository.save(new Member("media-sync-member@example.com", null));
        farm = farmRepository.save(new Farm(member, "미디어싱크농장", "충청북도 제천시"));
        crop = cropRepository.save(new Crop(7003, "감초", CropUsePartCategory.ROOT_BARK));
        memberCropRepository.save(new MemberCrop(member, farm, crop));
    }

    @Test
    @DisplayName("resending the same media id on update does not violate the unique constraint")
    void resending_the_same_media_id_on_update_does_not_violate_the_unique_constraint() throws Exception {
        var media = newMedia();
        var recordId = java.util.Objects.requireNonNull(farmingRecordService.create(wateringCommand(java.util.Arrays.asList(mediaId(media)))).getId());
        farmingRecordService.update(updateCommand(recordId, java.util.Arrays.asList(mediaId(media))));
        var rows = farmingRecordMediaRepository.findByRecord_Id(recordId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUploadedMedia().getId()).isEqualTo(mediaId(media));
        assertThat(refreshed(media).getStatus()).isEqualTo(UploadedMediaStatus.ATTACHED);
    }

    @Test
    @DisplayName("dropping a media id on update marks it deleted and keeps the retained one")
    void dropping_a_media_id_on_update_marks_it_deleted_and_keeps_the_retained_one() throws Exception {
        var kept = newMedia();
        var dropped = newMedia();
        var recordId = java.util.Objects.requireNonNull(farmingRecordService.create(wateringCommand(java.util.Arrays.asList(mediaId(kept),
            mediaId(dropped)))).getId());
        farmingRecordService.update(updateCommand(recordId, java.util.Arrays.asList(mediaId(kept))));
        var rows = farmingRecordMediaRepository.findByRecord_Id(recordId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUploadedMedia().getId()).isEqualTo(mediaId(kept));
        assertThat(refreshed(kept).getStatus()).isEqualTo(UploadedMediaStatus.ATTACHED);
        assertThat(refreshed(dropped).getStatus()).isEqualTo(UploadedMediaStatus.DELETED);
    }

    @Test
    @DisplayName("update rejects duplicate media ids in the same request")
    void update_rejects_duplicate_media_ids_in_the_same_request() throws Exception {
        var media = newMedia();
        var recordId = java.util.Objects.requireNonNull(farmingRecordService.create(wateringCommand(java.util.Collections.emptyList())).getId());
        assertThatThrownBy(() -> {
                farmingRecordService.update(updateCommand(recordId, java.util.Arrays.asList(mediaId(media), mediaId(media))));
        }).isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("update rejects media already attached to a different record")
    void update_rejects_media_already_attached_to_a_different_record() throws Exception {
        var otherMedia = newMedia();
        var otherRecordId = java.util.Objects.requireNonNull(farmingRecordService.create(wateringCommand(java.util.Arrays.asList(mediaId(otherMedia)))).getId());
        var recordId = java.util.Objects.requireNonNull(farmingRecordService.create(wateringCommand(java.util.Collections.emptyList())).getId());
        assertThat(otherRecordId).isNotEqualTo(recordId);
        assertThatThrownBy(() -> {
                farmingRecordService.update(updateCommand(recordId, java.util.Arrays.asList(mediaId(otherMedia))));
        }).isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.MEDIA_NOT_ATTACHABLE));
    }

    private UploadedMedia newMedia() {
        return uploadedMediaRepository.save(new UploadedMedia(member,
            UploadedMediaType.IMAGE,
            UploadedMediaUsageType.FARMING_RECORD,
            "https://example.com/farming-record-%s.jpg".formatted(UUID.randomUUID()),
            "farming-record-test-%s".formatted(UUID.randomUUID())));
    }

    private UUID mediaId(UploadedMedia media) {
        return java.util.Objects.requireNonNull(media.getId());
    }

    private UploadedMedia refreshed(UploadedMedia media) {
        return uploadedMediaRepository.findById(mediaId(media)).orElseThrow();
    }

    private FarmingRecordCommand.Create wateringCommand(List<UUID> mediaIds) {
        return new FarmingRecordCommand.Create(memberId(),
            farmId(),
            cropId(),
            WorkType.WATERING,
            LocalDateTime.of(2026,
            4,
            1,
            8,
            30),
            "맑음",
            18,
            "미디어 싱크 검증용 관수 기록입니다",
            null,
            new FarmingRecordCommand.WateringDetail(IrrigationAmount.NORMAL,
            IrrigationMethod.DRIP),
            null,
            null,
            null,
            null,
            mediaIds,
            EntryMode.MANUAL);
    }

    private FarmingRecordCommand.Update updateCommand(UUID recordId, List<UUID> mediaIds) {
        return new FarmingRecordCommand.Update(memberId(),
            recordId,
            farmId(),
            cropId(),
            WorkType.WATERING,
            LocalDateTime.of(2026,
            4,
            1,
            8,
            30),
            "맑음",
            19,
            "미디어 싱크 검증용 관수 기록 수정입니다",
            null,
            new FarmingRecordCommand.WateringDetail(IrrigationAmount.NORMAL,
            IrrigationMethod.DRIP),
            null,
            null,
            null,
            null,
            mediaIds);
    }
}
