package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.ManagementType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class CoachingContextProviderTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private final UUID farmId = UUID.fromString("10000000-0000-0000-0000-000000000042");
    private final UUID cropId = UUID.fromString("20000000-0000-0000-0000-000000000042");
    private final UUID recordId = UUID.fromString("40000000-0000-0000-0000-000000000001");
    @Mock private MemberRepository memberRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private CropRepository cropRepository;
    @Mock private FarmingRecordRepository farmingRecordRepository;
    private CoachingContextProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        provider = new CoachingContextProvider(memberRepository, farmRepository, cropRepository, farmingRecordRepository);
    }

    @Test
    @DisplayName("build rejects farm id outside current member ownership")
    void build_rejects_farm_id_outside_current_member_ownership() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member()));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> provider.build(new CoachingRagCommand(memberId, "농장 상태?", farmId, null, null, null, null, null)));
        assertEquals(ErrorCode.RAG_INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    @DisplayName("build rejects record id outside current member ownership")
    void build_rejects_record_id_outside_current_member_ownership() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member()));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> provider.build(new CoachingRagCommand(memberId, "일지 상태?", null, null, recordId, null, null, null)));
        assertEquals(ErrorCode.RAG_INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    @DisplayName("build uses owner scoped farm and member scoped record context")
    void build_uses_owner_scoped_farm_and_member_scoped_record_context() throws Exception {
        var member = member();
        var farm = farm(member);
        var crop = crop();
        var record = record(member, farm, crop);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(record);
        var context = provider.build(new CoachingRagCommand(memberId, "관수 요약?", farmId, cropId, recordId, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), null));
        assertThat(context.getText()).contains("- 영농 경력 점수: 72");
        assertThat(context.getText()).contains("- 경영 형태: AGRICULTURAL_INDIVIDUAL");
        assertThat(context.getText()).contains("- 농장: 하늘들 약초농장 (강원특별자치도 평창군 진부면 솔바람길 24)");
        assertThat(context.getText()).contains("- 작물: 참당귀 / 뿌리·껍질");
        assertThat(context.getText()).contains("- 기준 영농일지: 2026-06-22T07:50 관수");
        verify(farmRepository).findByIdAndOwnerId(farmId, memberId);
        verify(farmingRecordRepository).findByIdAndMember_Id(recordId, memberId);
    }
    private Member member() {
        return also(new Member(memberId, "farmer@example.com", null), migratedMember -> {
        migratedMember.setName("박민서");
        migratedMember.setExperienceLevel(72);
        migratedMember.setManagementType(ManagementType.AGRICULTURAL_INDIVIDUAL);
        });
    }
    private Farm farm(Member owner) {
        return new Farm(farmId, owner, "하늘들 약초농장", "강원특별자치도 평창군 진부면 솔바람길 24");
    }
    private Crop crop() {
        return new Crop(cropId, 422, "참당귀", CropUsePartCategory.ROOT_BARK);
    }
    private FarmingRecord record(Member member, Farm farm, Crop crop) {
        return new FarmingRecord(recordId, member, farm, crop, WorkType.WATERING, LocalDateTime.of(2026, 6, 22, 7, 50), "맑음", 24, "점적 관수를 진행했다.", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    }
    private static <T> T also(T value, java.util.function.Consumer<T> action) { action.accept(value); return value; }

}
