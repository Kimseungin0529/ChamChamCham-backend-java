package com.chamchamcham.application.policy.recommendation;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.policy.support.PolicyBenefitCategory;
import com.chamchamcham.application.policy.support.TextListJsonCodec;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.member.ManagementType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.policy.PolicyProgram;
import com.chamchamcham.domain.policy.PolicyProgramRepository;
import com.chamchamcham.domain.policy.PolicyRecommendation;
import com.chamchamcham.domain.policy.PolicyRecommendationQueryRepository;
import com.chamchamcham.domain.policy.PolicyRecommendationRepository;
import com.chamchamcham.domain.policy.PolicyRecommendationSort;
import com.chamchamcham.domain.policy.PolicySource;
import com.chamchamcham.domain.policy.PolicySyncJob;
import com.chamchamcham.domain.policy.PolicySyncJobRepository;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import com.chamchamcham.domain.policy.PolicySyncTriggerType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class PolicyRecommendationServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID latestJobId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID policyProgramId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID recommendationId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    @Mock private PolicySyncJobRepository policySyncJobRepository;
    @Mock private PolicyProgramRepository policyProgramRepository;
    @Mock private PolicyRecommendationRepository policyRecommendationRepository;
    @Mock private PolicyRecommendationQueryRepository policyRecommendationQueryRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private FarmRepository farmRepository;
    private PolicyRecommendationService service;
    private OpaqueCursorCodec cursorCodec;
    private Member member;
    private PolicySyncJob latestJob;
    private PolicyRegionMatcher regionMatcher;

    @BeforeEach
    void setUp() throws Exception {
        cursorCodec = new OpaqueCursorCodec();
        regionMatcher = new PolicyRegionMatcher();
        service = new PolicyRecommendationService(policySyncJobRepository, policyProgramRepository, policyRecommendationRepository, policyRecommendationQueryRepository, memberRepository, memberCropRepository, farmRepository, regionMatcher, new PolicyRecommendationScorer(regionMatcher), new TextListJsonCodec(), cursorCodec, Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneId.of("UTC")));
        member = new Member(memberId, "member@example.com", "hash");
        member.setBirthDate(LocalDate.of(1995, 1, 1));
        member.setExperienceLevel(2);
        member.setManagementType(ManagementType.AGRICULTURAL_INDIVIDUAL);
        latestJob = syncJob(latestJobId);
    }

    @Test
    @DisplayName("list recommendations returns empty page when no successful sync exists")
    void list_recommendations_returns_empty_page_when_no_successful_sync_exists() throws Exception {
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(null);
        var page = service.listRecommendations(memberId, null, 20, null, PolicyRecommendationSort.RECOMMENDED);
        assertThat(page.getItems()).isEmpty();
        assertThat(page.getNextCursor()).isNull();
        verifyNoInteractions(policyProgramRepository, policyRecommendationRepository, policyRecommendationQueryRepository);
    }

    @Test
    @DisplayName("list recommendations regenerates stale or missing member recommendations from latest sync job")
    void list_recommendations_regenerates_stale_or_missing_member_recommendations_from_latest_sync_job() throws Exception {
        var program = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var row = recommendation(program, recommendationId, new BigDecimal("96.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(latestJob);
        when(policyProgramRepository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", LocalDate.of(2026, 4, 1))).thenReturn(java.util.Arrays.asList(program));
        when(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(List.of());
        when(policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(null);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        givenPolicyProfileData();
        when(policyRecommendationQueryRepository.findPage(recommendationSearchCondition(null, PolicyRecommendationSort.RECOMMENDED, null, 21))).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(java.util.Arrays.asList(row)));
        var page = service.listRecommendations(memberId, null, 20, null, PolicyRecommendationSort.RECOMMENDED);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().getFirst().getPolicyProgramId()).isEqualTo(policyProgramId);
        assertThat(page.getItems().getFirst().getProgramTitle()).isEqualTo("청년 약용작물 지원");
        assertThat(page.getItems().getFirst().getReason()).isEqualTo("청년농 대상이고 재배 품목이 맞아요.");
        assertThat(page.getNextCursor()).isNull();
        verify(policyRecommendationRepository).deleteByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026");
        var savedRecommendations = capturedSavedRecommendations();
        assertThat(savedRecommendations).hasSize(1);
        assertThat(savedRecommendations.getFirst().getScore().doubleValue()).isGreaterThanOrEqualTo(70.0);
    }

    @Test
    @DisplayName("list recommendations regenerates when latest recommendations include a policy outside current candidates")
    void list_recommendations_regenerates_when_latest_recommendations_include_a_policy_outside_current_candidates() throws Exception {
        var secondPolicyProgramId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        var obsoletePolicyProgramId = UUID.fromString("00000000-0000-0000-0000-000000000399");
        var firstProgram = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var secondProgram = recommendableProgram(secondPolicyProgramId, "청년 특용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"SPECIAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var row = recommendation(firstProgram, recommendationId, new BigDecimal("96.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(latestJob);
        when(policyProgramRepository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", LocalDate.of(2026, 4, 1))).thenReturn(java.util.Arrays.asList(firstProgram, secondProgram));
        when(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(java.util.Arrays.asList(obsoletePolicyProgramId));
        when(policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(LocalDateTime.of(2026, 3, 1, 0, 0));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        givenPolicyProfileData();
        when(policyRecommendationQueryRepository.findPage(recommendationSearchCondition(null, PolicyRecommendationSort.RECOMMENDED, null, 21))).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(java.util.Arrays.asList(row)));
        service.listRecommendations(memberId, null, 20, null, PolicyRecommendationSort.RECOMMENDED);
        verify(policyRecommendationRepository).deleteByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026");
        var savedRecommendations = capturedSavedRecommendations();
        assertThat(savedRecommendations.stream().map(it -> it.getPolicyProgram().getId()).toList()).containsExactlyInAnyOrder(policyProgramId, secondPolicyProgramId);
    }

    @Test
    @DisplayName("list recommendations reuses latest recommendations when saved policies are a subset of candidates")
    void list_recommendations_reuses_latest_recommendations_when_saved_policies_are_a_subset_of_candidates() throws Exception {
        var ineligiblePolicyProgramId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        var eligibleProgram = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var ineligibleProgram = recommendableProgram(ineligiblePolicyProgramId, "전남 청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"전라남도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var row = recommendation(eligibleProgram, recommendationId, new BigDecimal("96.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(latestJob);
        when(policyProgramRepository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", LocalDate.of(2026, 4, 1))).thenReturn(java.util.Arrays.asList(eligibleProgram, ineligibleProgram));
        when(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(java.util.Arrays.asList(policyProgramId));
        when(policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(LocalDateTime.of(2026, 3, 1, 0, 0));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        givenPolicyProfileData();
        when(policyRecommendationQueryRepository.findPage(recommendationSearchCondition(null, PolicyRecommendationSort.RECOMMENDED, null, 21))).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(java.util.Arrays.asList(row)));
        var page = service.listRecommendations(memberId, null, 20, null, PolicyRecommendationSort.RECOMMENDED);
        assertThat(page.getItems()).hasSize(1);
        verify(policyRecommendationRepository, never()).deleteByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026");
        verify(policyRecommendationRepository, never()).saveAll(org.mockito.ArgumentMatchers.<Iterable<PolicyRecommendation>>any());
    }

    @Test
    @DisplayName("list recommendations loads farms before crops through policy fetch join queries")
    void list_recommendations_loads_farms_before_crops_through_policy_fetch_join_queries() throws Exception {
        var program = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var row = recommendation(program, recommendationId, new BigDecimal("96.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        givenReusableRecommendations(program);
        when(policyRecommendationQueryRepository.findPage(recommendationSearchCondition(null, PolicyRecommendationSort.RECOMMENDED, null, 21))).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(java.util.Arrays.asList(row)));
        service.listRecommendations(memberId, null, 20, null, PolicyRecommendationSort.RECOMMENDED);
        verify(farmRepository).findAllWithBoundaryCoordinatesByOwnerId(memberId);
        verify(farmRepository, never()).findByOwnerId(memberId);
        verify(memberCropRepository, never()).findByMemberId(memberId);
        var order = inOrder(farmRepository, memberCropRepository);
        {
        order.verify(farmRepository).findAllWithBoundaryCoordinatesByOwnerId(memberId);
        order.verify(memberCropRepository).findAllWithCropByMemberId(memberId);
        }
    }

    @Test
    @DisplayName("list recommendations saves only eligible generated recommendations")
    void list_recommendations_saves_only_eligible_generated_recommendations() throws Exception {
        var ineligiblePolicyProgramId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        var eligibleProgram = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var regionMismatchedProgram = recommendableProgram(ineligiblePolicyProgramId, "전남 청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"전라남도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var row = recommendation(eligibleProgram, recommendationId, new BigDecimal("96.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(latestJob);
        when(policyProgramRepository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", LocalDate.of(2026, 4, 1))).thenReturn(java.util.Arrays.asList(eligibleProgram, regionMismatchedProgram));
        when(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(List.of());
        when(policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(null);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        givenPolicyProfileData();
        when(policyRecommendationQueryRepository.findPage(recommendationSearchCondition(null, PolicyRecommendationSort.RECOMMENDED, null, 21))).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(java.util.Arrays.asList(row)));
        service.listRecommendations(memberId, null, 20, null, PolicyRecommendationSort.RECOMMENDED);
        var savedRecommendations = capturedSavedRecommendations();
        assertThat(savedRecommendations.stream().map(it -> it.getPolicyProgram().getId()).toList()).containsExactly(policyProgramId);
    }

    @Test
    @DisplayName("list recommendations passes benefit category and latest sort to query repository")
    void list_recommendations_passes_benefit_category_and_latest_sort_to_query_repository() throws Exception {
        var program = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(latestJob);
        when(policyProgramRepository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", LocalDate.of(2026, 4, 1))).thenReturn(java.util.Arrays.asList(program));
        when(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(java.util.Arrays.asList(policyProgramId));
        when(policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(LocalDateTime.of(2026, 3, 1, 0, 0));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        givenPolicyProfileData();
        var expectedCondition = recommendationSearchCondition("융자·금융", PolicyRecommendationSort.LATEST, null, 21);
        when(policyRecommendationQueryRepository.findPage(expectedCondition)).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(List.of()));
        service.listRecommendations(memberId, null, 20, PolicyBenefitCategory.FINANCE, PolicyRecommendationSort.LATEST);
        var captor = ArgumentCaptor.forClass(PolicyRecommendationQueryRepository.SearchCondition.class);
        verify(policyRecommendationQueryRepository).findPage((captor.capture() != null ? captor.capture() : expectedCondition));
        assertThat(captor.getValue().benefitSummary()).isEqualTo("융자·금융");
        assertThat(captor.getValue().sort()).isEqualTo(PolicyRecommendationSort.LATEST);
    }

    @Test
    @DisplayName("recommended next cursor contains only recommended ordering fields and round trips to query cursor")
    void recommended_next_cursor_contains_only_recommended_ordering_fields_and_round_trips_to_query_cursor() throws Exception {
        var secondProgramId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        var secondRecommendationId = UUID.fromString("00000000-0000-0000-0000-000000000402");
        var firstProgram = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var secondProgram = recommendableProgram(secondProgramId, "후속 정책", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var firstRow = recommendation(firstProgram, recommendationId, new BigDecimal("96.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        var secondRow = recommendation(secondProgram, secondRecommendationId, new BigDecimal("90.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        givenReusableRecommendations(firstProgram);
        var firstCondition = recommendationSearchCondition(null, PolicyRecommendationSort.RECOMMENDED, null, 2);
        var cursorCondition = recommendationSearchCondition(null, PolicyRecommendationSort.RECOMMENDED, new PolicyRecommendationQueryRepository.Cursor(new BigDecimal("96.0"), null, LocalDate.of(2026, 6, 30), recommendationId), 2);
        when(policyRecommendationQueryRepository.findPage(firstCondition)).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(java.util.Arrays.asList(firstRow, secondRow)));
        when(policyRecommendationQueryRepository.findPage(cursorCondition)).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(List.of()));
        var firstPage = service.listRecommendations(memberId, null, 1, null, PolicyRecommendationSort.RECOMMENDED);
        var payload = cursorCodec.decode(Objects.requireNonNull(firstPage.getNextCursor()), PolicyRecommendationCursorPayload.class);
        assertThat(payload).isEqualTo(new PolicyRecommendationCursorPayload(PolicySource.NONGUP_EZ, "2026", null, PolicyRecommendationSort.RECOMMENDED, new BigDecimal("96.0"), null, LocalDate.of(2026, 6, 30), recommendationId));
        service.listRecommendations(memberId, firstPage.getNextCursor(), 1, null, PolicyRecommendationSort.RECOMMENDED);
        verify(policyRecommendationQueryRepository).findPage(cursorCondition);
    }

    @Test
    @DisplayName("latest next cursor contains only latest ordering fields and round trips to query cursor")
    void latest_next_cursor_contains_only_latest_ordering_fields_and_round_trips_to_query_cursor() throws Exception {
        var secondProgramId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        var secondRecommendationId = UUID.fromString("00000000-0000-0000-0000-000000000402");
        var firstProgram = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var secondProgram = recommendableProgram(secondProgramId, "후속 정책", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        var firstRow = recommendation(firstProgram, recommendationId, new BigDecimal("96.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        var secondRow = recommendation(secondProgram, secondRecommendationId, new BigDecimal("90.0"), LocalDateTime.of(2026, 3, 15, 9, 0));
        givenReusableRecommendations(firstProgram);
        var firstCondition = recommendationSearchCondition(null, PolicyRecommendationSort.LATEST, null, 2);
        var cursorCondition = recommendationSearchCondition(null, PolicyRecommendationSort.LATEST, new PolicyRecommendationQueryRepository.Cursor(null, LocalDate.of(2026, 3, 1), null, recommendationId), 2);
        when(policyRecommendationQueryRepository.findPage(firstCondition)).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(java.util.Arrays.asList(firstRow, secondRow)));
        when(policyRecommendationQueryRepository.findPage(cursorCondition)).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(List.of()));
        var firstPage = service.listRecommendations(memberId, null, 1, null, PolicyRecommendationSort.LATEST);
        var payload = cursorCodec.decode(Objects.requireNonNull(firstPage.getNextCursor()), PolicyRecommendationCursorPayload.class);
        assertThat(payload).isEqualTo(new PolicyRecommendationCursorPayload(PolicySource.NONGUP_EZ, "2026", null, PolicyRecommendationSort.LATEST, null, LocalDate.of(2026, 3, 1), null, recommendationId));
        service.listRecommendations(memberId, firstPage.getNextCursor(), 1, null, PolicyRecommendationSort.LATEST);
        verify(policyRecommendationQueryRepository).findPage(cursorCondition);
    }

    @Test
    @DisplayName("list recommendations rejects recommended cursor without score before downstream reads")
    void list_recommendations_rejects_recommended_cursor_without_score_before_downstream_reads() throws Exception {
        var cursor = cursorCodec.encode(new PolicyRecommendationCursorPayload(PolicySource.NONGUP_EZ, "2026", null, PolicyRecommendationSort.RECOMMENDED, null, null, LocalDate.of(2026, 6, 30), recommendationId));
        givenLatestSyncJob();
        var exception = assertThrows(BusinessException.class, () -> service.listRecommendations(memberId, cursor, 20, null, PolicyRecommendationSort.RECOMMENDED));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoRecommendationReads();
    }

    @Test
    @DisplayName("latest cursor with null start date reaches query as null group boundary")
    void latest_cursor_with_null_start_date_reaches_query_as_null_group_boundary() throws Exception {
        var program = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", null, LocalDate.of(2026, 6, 30), "{}");
        givenReusableRecommendations(program);
        var cursor = cursorCodec.encode(new PolicyRecommendationCursorPayload(PolicySource.NONGUP_EZ, "2026", null, PolicyRecommendationSort.LATEST, null, null, null, recommendationId));
        var expectedCondition = recommendationSearchCondition(null, PolicyRecommendationSort.LATEST, new PolicyRecommendationQueryRepository.Cursor(null, null, null, recommendationId), 21);
        when(policyRecommendationQueryRepository.findPage(expectedCondition)).thenReturn(new PolicyRecommendationQueryRepository.SearchResult(List.of()));
        service.listRecommendations(memberId, cursor, 20, null, PolicyRecommendationSort.LATEST);
        verify(policyRecommendationQueryRepository).findPage(expectedCondition);
    }

    @Test
    @DisplayName("list recommendations rejects cursor with unknown source before downstream reads")
    void list_recommendations_rejects_cursor_with_unknown_source_before_downstream_reads() throws Exception {
        var cursor = cursorCodec.encode(map(new AbstractMap.SimpleEntry<>("source", "OTHER_SOURCE"), new AbstractMap.SimpleEntry<>("sourceYear", "2026"), new AbstractMap.SimpleEntry<>("benefitCategory", null), new AbstractMap.SimpleEntry<>("sort", "RECOMMENDED"), new AbstractMap.SimpleEntry<>("score", "80.0"), new AbstractMap.SimpleEntry<>("applyStartsOn", null), new AbstractMap.SimpleEntry<>("applyEndsOn", "2026-06-30"), new AbstractMap.SimpleEntry<>("id", recommendationId.toString())));
        givenLatestSyncJob();
        var exception = assertThrows(BusinessException.class, () -> service.listRecommendations(memberId, cursor, 20, null, PolicyRecommendationSort.RECOMMENDED));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoRecommendationReads();
    }

    @Test
    @DisplayName("list recommendations rejects cursor bound to different benefit category before downstream reads")
    void list_recommendations_rejects_cursor_bound_to_different_benefit_category_before_downstream_reads() throws Exception {
        var cursor = cursorCodec.encode(new PolicyRecommendationCursorPayload(PolicySource.NONGUP_EZ, "2026", PolicyBenefitCategory.GRANT.name(), PolicyRecommendationSort.RECOMMENDED, new BigDecimal("80.0"), null, LocalDate.of(2026, 6, 30), recommendationId));
        givenLatestSyncJob();
        var exception = assertThrows(BusinessException.class, () -> {
        service.listRecommendations(memberId, cursor, 20, PolicyBenefitCategory.FINANCE, PolicyRecommendationSort.RECOMMENDED);
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoRecommendationReads();
    }

    @Test
    @DisplayName("list recommendations rejects cursor bound to a different sort before downstream reads")
    void list_recommendations_rejects_cursor_bound_to_a_different_sort_before_downstream_reads() throws Exception {
        var mismatchedCursor = cursorCodec.encode(new PolicyRecommendationCursorPayload(PolicySource.NONGUP_EZ, "2026", null, PolicyRecommendationSort.RECOMMENDED, new BigDecimal("80.0"), null, LocalDate.of(2026, 6, 30), recommendationId));
        givenLatestSyncJob();
        var exception = assertThrows(BusinessException.class, () -> {
        service.listRecommendations(memberId, mismatchedCursor, 20, null, PolicyRecommendationSort.LATEST);
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoRecommendationReads();
    }

    @Test
    @DisplayName("list recommendations rejects cursor from stale sync job as invalid input")
    void list_recommendations_rejects_cursor_from_stale_sync_job_as_invalid_input() throws Exception {
        var staleCursor = cursorCodec.encode(new PolicyRecommendationCursorPayload(PolicySource.NONGUP_EZ, "2025", null, PolicyRecommendationSort.RECOMMENDED, new BigDecimal("80.0"), null, LocalDate.of(2026, 6, 30), recommendationId));
        givenLatestSyncJob();
        var exception = assertThrows(BusinessException.class, () -> service.listRecommendations(memberId, staleCursor, 20, null, PolicyRecommendationSort.RECOMMENDED));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoRecommendationReads();
    }

    @Test
    @DisplayName("list recommendations rejects malformed cursor as invalid input")
    void list_recommendations_rejects_malformed_cursor_as_invalid_input() throws Exception {
        givenLatestSyncJob();
        var exception = assertThrows(BusinessException.class, () -> service.listRecommendations(memberId, "not-a-valid-cursor", 20, null, PolicyRecommendationSort.RECOMMENDED));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoRecommendationReads();
    }

    @Test
    @DisplayName("list recommendations surfaces malformed stored tag json")
    void list_recommendations_surfaces_malformed_stored_tag_json() throws Exception {
        var program = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "{not-json", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{}");
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(latestJob);
        when(policyProgramRepository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", LocalDate.of(2026, 4, 1))).thenReturn(java.util.Arrays.asList(program));
        when(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(List.of());
        when(policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(null);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        givenPolicyProfileData();
        assertThrows(IllegalArgumentException.class, () -> service.listRecommendations(memberId, null, 20, null, PolicyRecommendationSort.RECOMMENDED));
        verifyNoInteractions(policyRecommendationQueryRepository);
    }

    @Test
    @DisplayName("get program detail returns synced recommendable detail with contacts and attachments from raw payload")
    void get_program_detail_returns_synced_recommendable_detail_with_contacts_and_attachments_from_raw_payload() throws Exception {
        var program = recommendableProgram(policyProgramId, "청년 약용작물 지원", "[\"YOUNG_FARMER\",\"REGISTERED_FARMER\"]", "[\"MEDICINAL_CROP\"]", "[\"충청북도\"]", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "{\n  \"bizPicList\": [\n    {\"bizTkcgInstCdNm\": \"농림축산식품부\", \"bizTkcgDeptInstCdNm\": \"친환경농업과\", \"bizPicTelno\": \"044-201-2434\"}\n  ],\n  \"bizAtchFileList\": [\n    {\"originalName\": \"시행지침.pdf\", \"extension\": \"pdf\", \"size\": 1537272, \"url\": \"https://example.test/file.pdf\"}\n  ]\n}");
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(policyProgramRepository.findByIdAndDetailSyncedTrueAndRecommendableTrue(policyProgramId)).thenReturn(program);
        var detail = service.getProgramDetail(memberId, policyProgramId);
        assertThat(detail.getId()).isEqualTo(policyProgramId);
        assertThat(detail.getProgramTitle()).isEqualTo("청년 약용작물 지원");
        assertThat(detail.getContacts().getFirst().getDepartmentName()).isEqualTo("친환경농업과");
        assertThat(detail.getContacts().getFirst().getPhoneNumber()).isEqualTo("044-201-2434");
        assertThat(detail.getAttachments().getFirst().getFileName()).isEqualTo("시행지침.pdf");
        assertThat(detail.getAttachments().getFirst().getSizeBytes()).isEqualTo(1537272L);
    }

    @Test
    @DisplayName("get program detail hides list only or non recommendable policies")
    void get_program_detail_hides_list_only_or_non_recommendable_policies() throws Exception {
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(policyProgramRepository.findByIdAndDetailSyncedTrueAndRecommendableTrue(policyProgramId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> service.getProgramDetail(memberId, policyProgramId));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_PROGRAM_NOT_FOUND);
    }
    @SuppressWarnings("unchecked")
    private List<PolicyRecommendation> capturedSavedRecommendations() {
        
        var captor = (ArgumentCaptor<Iterable<PolicyRecommendation>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Iterable.class);
        verify(policyRecommendationRepository).saveAll(captor.capture());
        var saved = new ArrayList<PolicyRecommendation>();
        captor.getValue().forEach(saved::add);
        return saved;
    }
    private PolicyRecommendationQueryRepository.SearchCondition recommendationSearchCondition(String benefitSummary, PolicyRecommendationSort sort, PolicyRecommendationQueryRepository.Cursor cursor, int size) {
        return new PolicyRecommendationQueryRepository.SearchCondition(memberId, PolicySource.NONGUP_EZ, "2026", benefitSummary, sort, cursor, size);
    }
    private void givenLatestSyncJob() {
        when(policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED)).thenReturn(latestJob);
    }
    private void givenReusableRecommendations(PolicyProgram... programs) {
        givenLatestSyncJob();
        when(policyProgramRepository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", LocalDate.of(2026, 4, 1))).thenReturn(Arrays.asList(programs));
        when(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(Arrays.stream(programs).map(it -> Objects.requireNonNull(it.getId())).toList());
        when(policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, PolicySource.NONGUP_EZ, "2026")).thenReturn(LocalDateTime.of(2026, 3, 1, 0, 0));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        givenPolicyProfileData();
    }
    private void verifyNoRecommendationReads() {
        verifyNoInteractions(policyProgramRepository, policyRecommendationRepository, memberRepository, memberCropRepository, farmRepository, policyRecommendationQueryRepository);
    }
    private void givenPolicyProfileData() {
        var farm = farm();
        var crop = new Crop(1001, "참당귀", CropUsePartCategory.ROOT_BARK);
        when(memberCropRepository.findAllWithCropByMemberId(memberId)).thenReturn(java.util.Arrays.asList(new MemberCrop(member, farm, crop)));
        when(farmRepository.findAllWithBoundaryCoordinatesByOwnerId(memberId)).thenReturn(java.util.Arrays.asList(farm));
    }
    private Farm farm() {
        var farm = new Farm(member, "제천 약초밭", "충청북도 제천시 청풍면 약초로 1");
        farm.setJibunAddress("충북 제천시 청풍면");
        return farm;
    }
    private PolicySyncJob syncJob(UUID id) {
        return new PolicySyncJob(id, PolicySource.NONGUP_EZ, "2026", PolicySyncTriggerType.SCHEDULED, PolicySyncJobStatus.SUCCEEDED, LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 4, 1, 0, 1), 0, 0, 0, 0, null, null);
    }
    private PolicyProgram recommendableProgram(UUID id, String title, String targetTagsJson, String cropTagsJson, String regionTagsJson, LocalDate applyStartsOn, LocalDate applyEndsOn, String rawPayload) {
        return new PolicyProgram(id, title, "약용작물 재배 청년농 지원", "충청북도", ManagementType.AGRICULTURAL_INDIVIDUAL, applyStartsOn, applyEndsOn, "https://example.test/policy", PolicySource.NONGUP_EZ, "AB000009", "2026", "약용작물 지원", "농림축산식품부", "친환경농업과", true, "https://example.test/apply", "2026.03.01~06.30", null, "만 40세 미만 청년농", "청년농 대상", "약용작물 생산 지원", "생산비 지원", "약용작물 재배 확대", "온라인 신청", "신청서", "자격 확인", true, true, targetTagsJson, cropTagsJson, regionTagsJson, rawPayload);
    }
    private PolicyRecommendation recommendation(PolicyProgram program, UUID id, BigDecimal score, LocalDateTime createdAt) {
        var result = new PolicyRecommendation(id, member, program, score, "청년농 대상이고 재배 품목이 맞아요.");
        setCreatedAt(result, createdAt);
        return result;
    }
    private void setCreatedAt(PolicyRecommendation entity, LocalDateTime createdAt) {
        try {
            var field = BaseTimeEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    @SafeVarargs
    private static <K,V> Map<K,V> map(Map.Entry<K,V>... entries) {
        var result = new LinkedHashMap<K,V>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }
}
