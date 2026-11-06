package com.chamchamcham.api.report;

import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.ApiApplication;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.application.report.FarmingCycleReportProjectionService;
import com.chamchamcham.application.report.ReportScope;
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
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
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
        "spring.datasource.url=jdbc:h2:mem:farming-record-projection-rollback;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    }
)
@ActiveProfiles("test")
class FarmingRecordProjectionRollbackIntegrationTest {
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
private UploadedMedia media;
private UUID memberId() { return java.util.Objects.requireNonNull(member.getId()); }
private UUID farmId() { return java.util.Objects.requireNonNull(farm.getId()); }
private UUID cropId() { return java.util.Objects.requireNonNull(crop.getId()); }
private UUID mediaId() { return java.util.Objects.requireNonNull(media.getId()); }
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
member = memberRepository.save(new Member("rollback-member@example.com", null));
farm = farmRepository.save(new Farm(member, "롤백검증농장", "충청북도 제천시"));
crop = cropRepository.save(new Crop(7002, "감초", CropUsePartCategory.ROOT_BARK));
memberCropRepository.save(new MemberCrop(member, farm, crop));
media = uploadedMediaRepository.save(new UploadedMedia(member, UploadedMediaType.IMAGE, UploadedMediaUsageType.FARMING_RECORD, "https://example.com/farming-record.jpg", "farming-record-test"));
}
@Test
@DisplayName("projection failure rolls back record detail and media rows")
void projection_failure_rolls_back_record_detail_and_media_rows() throws Exception {
doThrow(new IllegalStateException("projection failed"))
.when(projectionService)
.rebuild(new ReportScope(memberId(), farmId(), cropId()));
assertThatThrownBy(() -> {
farmingRecordService.create(wateringCommand(true));
}).isInstanceOf(IllegalStateException.class);
assertThat(farmingRecordRepository.findAll()).isEmpty();
assertThat(wateringRecordRepository.findAll()).isEmpty();
assertThat(farmingRecordMediaRepository.findAll()).isEmpty();
assertThat(recordFeedbackRepository.findAll()).isEmpty();
}
private FarmingRecordCommand.Create wateringCommand(boolean withMedia) {
return new FarmingRecordCommand.Create(memberId(), farmId(), cropId(), WorkType.WATERING, LocalDateTime.of(2026, 4, 1, 8, 30), "맑음", 18, "롤백 관수", null, new FarmingRecordCommand.WateringDetail(IrrigationAmount.NORMAL, IrrigationMethod.DRIP), null, null, null, null, (withMedia ? java.util.Arrays.asList(mediaId()) : java.util.Collections.emptyList()), EntryMode.MANUAL);
}
}
