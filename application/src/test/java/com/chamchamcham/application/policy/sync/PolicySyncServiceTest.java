package com.chamchamcham.application.policy.sync;

import com.chamchamcham.application.policy.source.nongupez.NongupEzPolicyContact;
import com.chamchamcham.application.policy.source.nongupez.NongupEzPolicyDetail;
import com.chamchamcham.application.policy.source.nongupez.NongupEzPolicyListItem;
import com.chamchamcham.application.policy.source.nongupez.NongupEzPolicySourceClient;
import com.chamchamcham.application.policy.support.NongupEzPolicyTagExtractor;
import com.chamchamcham.application.policy.support.PolicyBenefitCategory;
import com.chamchamcham.application.policy.support.PolicyCardTextGenerator;
import com.chamchamcham.application.policy.support.TextListJsonCodec;
import com.chamchamcham.domain.policy.PolicyProgram;
import com.chamchamcham.domain.policy.PolicyProgramRepository;
import com.chamchamcham.domain.policy.PolicySource;
import com.chamchamcham.domain.policy.PolicySyncJob;
import com.chamchamcham.domain.policy.PolicySyncJobRepository;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class PolicySyncServiceTest {
    private final UUID adminMemberId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    @Mock private NongupEzPolicySourceClient sourceClient;
    @Mock private PolicyProgramRepository policyProgramRepository;
    @Mock private PolicySyncJobRepository policySyncJobRepository;
    private PolicySyncService service;
    private PolicySyncJob persistedJob;

    @BeforeEach
    void setUp() throws Exception {
        service = new PolicySyncService(sourceClient, policyProgramRepository, policySyncJobRepository, new PolicyCardTextGenerator(), new NongupEzPolicyTagExtractor(), new TextListJsonCodec(), new TransactionTemplate(new TestTransactionManager()), Clock.systemDefaultZone());
    }

    @Test
    @DisplayName("runSync upserts list and detail success into recommendable policy")
    void runSync_upserts_list_and_detail_success_into_recommendable_policy() throws Exception {
        var listItem = listItem("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인에게 직불금을 지원합니다.", "{\"afbzCd\":\"AB000009\"}");
        when(sourceClient.detectLatestYear()).thenAnswer(it -> {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        return "2026";
        });
        when(sourceClient.fetchPrograms("2026")).thenAnswer(it -> {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        return java.util.Arrays.asList(listItem);
        });
        when(sourceClient.fetchDetail("AB000009", "2026")).thenAnswer(it -> {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        return detail("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인 지원", "농업경영정보를 등록하고 친환경인증을 받은 농업인", "인증단계별 직불금 지원", "{\"afbzCd\":\"AB000009\",\"bizYr\":\"2026\"}");
        });
        when(policyProgramRepository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2026")).thenReturn(List.of());
        stubJobSaveAndFind();
        var result = service.runScheduledSync();
        assertEquals(PolicySyncJobStatus.SUCCEEDED, result.getStatus());
        var savedProgram = capturedProgram();
        assertTrue(savedProgram.getDetailSynced());
        assertTrue(savedProgram.getRecommendable());
        assertTrue(savedProgram.getEligibilitySummary().length()<= 19);
        assertEquals("https://www.nongupez.go.kr/nsm/bizAply/wholeBiz/wholeBizDtls?afbzCd=AB000009&bizYr=2026", savedProgram.getSourceUrl());
    }

    @Test
    @DisplayName("sync loads existing policies once for source year instead of per policy")
    void sync_loads_existing_policies_once_for_source_year_instead_of_per_policy() throws Exception {
        var existing = existingSyncedProgram(PolicyBenefitCategory.GRANT.getLabel());
        var newItem = listItem("AB000010", "2026", "특용작물 시설 지원", "특용작물 재배 시설을 지원합니다.", "{\"afbzCd\":\"AB000010\"}");
        when(sourceClient.detectLatestYear()).thenReturn("2026");
        when(sourceClient.fetchPrograms("2026")).thenReturn(java.util.Arrays.asList(listItem("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인에게 직불금을 지원합니다.", "{\"afbzCd\":\"AB000009\"}"), newItem));
        when(sourceClient.fetchDetail("AB000009", "2026")).thenReturn(detail("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인 지원", "농업경영정보를 등록하고 친환경인증을 받은 농업인", "인증단계별 직불금 지원", "{\"afbzCd\":\"AB000009\",\"bizYr\":\"2026\"}"));
        when(sourceClient.fetchDetail("AB000010", "2026")).thenReturn(detail("AB000010", "2026", "특용작물 시설 지원", "특용작물 재배 시설 지원", "농업경영정보를 등록한 특용작물 재배 농업인", "시설 설치비 지원", "{\"afbzCd\":\"AB000010\",\"bizYr\":\"2026\"}"));
        when(policyProgramRepository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2026")).thenReturn(java.util.Arrays.asList(existing));
        stubJobSaveAndFind();
        service.runScheduledSync();
        verify(policyProgramRepository, times(1)).findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2026");
    }

    @Test
    @DisplayName("detail failure keeps job succeeded and policy non recommendable")
    void detail_failure_keeps_job_succeeded_and_policy_non_recommendable() throws Exception {
        when(sourceClient.detectLatestYear()).thenReturn("2026");
        when(sourceClient.fetchPrograms("2026")).thenReturn(java.util.Arrays.asList(listItem("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인에게 직불금을 지원합니다.", "{\"afbzCd\":\"AB000009\"}")));
        when(sourceClient.fetchDetail("AB000009", "2026")).thenThrow(new RuntimeException("detail down"));
        when(policyProgramRepository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2026")).thenReturn(List.of());
        stubJobSaveAndFind();
        var result = service.runScheduledSync();
        assertEquals(PolicySyncJobStatus.SUCCEEDED, result.getStatus());
        assertEquals(1, persistedJob.getDetailFailureCount());
        assertEquals(0, persistedJob.getDetailSuccessCount());
        assertFalse(capturedProgram().getDetailSynced());
    }

    @Test
    @DisplayName("unchanged existing policy is not saved again")
    void unchanged_existing_policy_is_not_saved_again() throws Exception {
        var existing = existingSyncedProgram(PolicyBenefitCategory.GRANT.getLabel());
        when(sourceClient.detectLatestYear()).thenReturn("2026");
        when(sourceClient.fetchPrograms("2026")).thenReturn(java.util.Arrays.asList(listItem("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인에게 직불금을 지원합니다.", "{\"afbzCd\":\"AB000009\"}")));
        when(sourceClient.fetchDetail("AB000009", "2026")).thenReturn(detail("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인 지원", "농업경영정보를 등록하고 친환경인증을 받은 농업인", "인증단계별 직불금 지원", "{\"afbzCd\":\"AB000009\",\"bizYr\":\"2026\"}"));
        when(policyProgramRepository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2026")).thenReturn(java.util.Arrays.asList(existing));
        stubJobSaveAndFind();
        service.runScheduledSync();
        verify(policyProgramRepository, never()).save(existing);
    }

    @Test
    @DisplayName("sync replaces legacy stored benefit summary with canonical label")
    void sync_replaces_legacy_stored_benefit_summary_with_canonical_label() throws Exception {
        var existing = existingSyncedProgram("직불/수당");
        when(sourceClient.detectLatestYear()).thenReturn("2026");
        when(sourceClient.fetchPrograms("2026")).thenReturn(java.util.Arrays.asList(listItem("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인에게 직불금을 지원합니다.", "{\"afbzCd\":\"AB000009\"}")));
        when(sourceClient.fetchDetail("AB000009", "2026")).thenReturn(detail("AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인 지원", "농업경영정보를 등록하고 친환경인증을 받은 농업인", "인증단계별 직불금 지원", "{\"afbzCd\":\"AB000009\",\"bizYr\":\"2026\"}"));
        when(policyProgramRepository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2026")).thenReturn(java.util.Arrays.asList(existing));
        stubJobSaveAndFind();
        service.runScheduledSync();
        assertEquals(PolicyBenefitCategory.GRANT.getLabel(), existing.getBenefitSummary());
        verify(policyProgramRepository).save(existing);
    }

    @Test
    @DisplayName("policy missing from current source list becomes not recommendable")
    void policy_missing_from_current_source_list_becomes_not_recommendable() throws Exception {
        var missing = existingSyncedProgram(PolicyBenefitCategory.GRANT.getLabel());
        when(sourceClient.detectLatestYear()).thenReturn("2026");
        when(sourceClient.fetchPrograms("2026")).thenReturn(List.of());
        when(policyProgramRepository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2026")).thenReturn(java.util.Arrays.asList(missing));
        stubJobSaveAndFind();
        service.runScheduledSync();
        assertFalse(missing.getRecommendable());
        verify(policyProgramRepository).save(missing);
    }

    @Test
    @DisplayName("whole source failure records failed job and keeps previous policies")
    void whole_source_failure_records_failed_job_and_keeps_previous_policies() throws Exception {
        when(sourceClient.detectLatestYear()).thenThrow(new RuntimeException("condition down"));
        when(policySyncJobRepository.save(any(PolicySyncJob.class))).thenAnswer(invocation -> {
        var job = ((PolicySyncJob) invocation.getArguments()[0]);
        persistedJob = new PolicySyncJob(jobId, job.getSource(), job.getTargetYear(), job.getTriggerType(), job.getStatus(), job.getStartedAt(), job.getFinishedAt(), job.getTotalCount(), job.getSyncedCount(), job.getDetailSuccessCount(), job.getDetailFailureCount(), job.getErrorMessage(), job.getCreatedByMemberId());
        return persistedJob;
        });
        var result = service.createAdminSyncJob(adminMemberId);
        assertEquals(PolicySyncJobStatus.FAILED, result.getStatus());
        verify(policyProgramRepository, never()).deleteAll();
    }
    private void stubJobSaveAndFind() {
        when(policySyncJobRepository.save(any(PolicySyncJob.class))).thenAnswer(invocation -> {
        var job = ((PolicySyncJob) invocation.getArguments()[0]);
        persistedJob = new PolicySyncJob(jobId, job.getSource(), job.getTargetYear(), job.getTriggerType(), job.getStatus(), job.getStartedAt(), null, 0, 0, 0, 0, null, job.getCreatedByMemberId());
        return persistedJob;
        });
        when(policySyncJobRepository.findById(jobId)).thenAnswer(it -> Optional.of(persistedJob));
    }
    private PolicyProgram capturedProgram() {
        var captor = ArgumentCaptor.forClass(PolicyProgram.class);
        verify(policyProgramRepository).save(captor.capture());
        return captor.getValue();
    }
    private NongupEzPolicyListItem listItem(String externalId, String sourceYear, String title, String summary, String rawJson) {
        return new NongupEzPolicyListItem(externalId, sourceYear, title, summary, "농림축산식품부", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), rawJson);
    }
    private NongupEzPolicyDetail detail(String externalId, String sourceYear, String title, String summary, String eligibility, String benefit, String rawJson) {
        return new NongupEzPolicyDetail(externalId, sourceYear, title, "친환경농업 확산", summary, eligibility, benefit, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31), "방문 신청", "신청서", "자격 확인", "농림축산식품부", java.util.Arrays.asList(new NongupEzPolicyContact("농림축산식품부", "친환경농업과", "044-000-0000")), List.of(), rawJson);
    }
    private PolicyProgram existingSyncedProgram(String benefitSummary) {
        var it = new PolicyProgram("친환경농업 직불 지원", "친환경농업 확산\n\n친환경 인증 농업인 지원\n\n농업경영정보를 등록하고 친환경인증을 받은 농업인\n\n인증단계별 직불금 지원", "전국", null);
        it.applyListFields(PolicySource.NONGUP_EZ, "AB000009", "2026", "친환경농업 직불 지원", "친환경 인증 농업인에게 직불금을 지원합니다.", "전국", "https://www.nongupez.go.kr/nsm/bizAply/wholeBiz/wholeBizDtls?afbzCd=AB000009&bizYr=2026", "농림축산식품부");
        it.applyDetailFields("친환경농업 확산\n\n친환경 인증 농업인 지원\n\n농업경영정보를 등록하고 친환경인증을 받은 농업인\n\n인증단계별 직불금 지원", "친환경농업 확산", "농업경영정보를 등록하고 친환경인증을 받은 농업인", "인증 보유 농업인", "인증단계별 직불금 지원", benefitSummary, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31), "2026.02.01~03.31", null, "방문 신청", "신청서", "자격 확인", "친환경농업과", false, null, "[\"REGISTERED_FARMER\"]", "[\"ECO_FRIENDLY\"]", "[\"전국\"]", "{\"afbzCd\":\"AB000009\",\"bizYr\":\"2026\"}", true);
        return it;
    }
    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }
}
