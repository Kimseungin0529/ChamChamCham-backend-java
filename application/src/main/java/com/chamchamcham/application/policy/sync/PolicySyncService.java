package com.chamchamcham.application.policy.sync;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.policy.source.nongupez.*;
import com.chamchamcham.application.policy.support.*;
import com.chamchamcham.domain.policy.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.util.*;

@Service
public class PolicySyncService {
    private static final String DEFAULT_REGION = "전국";
    private static final String UNKNOWN_PERIOD_LABEL = "접수기관문의";
    private final NongupEzPolicySourceClient sourceClient;
    private final PolicyProgramRepository policyProgramRepository;
    private final PolicySyncJobRepository policySyncJobRepository;
    private final PolicyCardTextGenerator cardTextGenerator;
    private final NongupEzPolicyTagExtractor tagExtractor;
    private final TextListJsonCodec textListJsonCodec;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    @Autowired(required = false)
    public PolicySyncService(NongupEzPolicySourceClient sourceClient, PolicyProgramRepository policyProgramRepository, PolicySyncJobRepository policySyncJobRepository, PolicyCardTextGenerator cardTextGenerator, NongupEzPolicyTagExtractor tagExtractor, TextListJsonCodec textListJsonCodec, TransactionTemplate transactionTemplate) {
        this(sourceClient, policyProgramRepository, policySyncJobRepository, cardTextGenerator, tagExtractor, textListJsonCodec, transactionTemplate, Clock.systemDefaultZone());
    }
    @Autowired(required = false)
    public PolicySyncService(NongupEzPolicySourceClient sourceClient, PolicyProgramRepository policyProgramRepository, PolicySyncJobRepository policySyncJobRepository, PolicyCardTextGenerator cardTextGenerator, NongupEzPolicyTagExtractor tagExtractor, TextListJsonCodec textListJsonCodec, TransactionTemplate transactionTemplate, Clock clock) {
        this.sourceClient = Objects.requireNonNull(sourceClient, "sourceClient");
        this.policyProgramRepository = Objects.requireNonNull(policyProgramRepository, "policyProgramRepository");
        this.policySyncJobRepository = Objects.requireNonNull(policySyncJobRepository, "policySyncJobRepository");
        this.cardTextGenerator = Objects.requireNonNull(cardTextGenerator, "cardTextGenerator");
        this.tagExtractor = Objects.requireNonNull(tagExtractor, "tagExtractor");
        this.textListJsonCodec = Objects.requireNonNull(textListJsonCodec, "textListJsonCodec");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
        this.clock = Objects.requireNonNull(clock, "clock");
    }
    public PolicySyncResult.JobSummary createAdminSyncJob(UUID adminMemberId) {
        Objects.requireNonNull(adminMemberId, "adminMemberId");
        return PolicySyncResult.JobSummary.from(createJobWithDetectedYear(PolicySyncTriggerType.ADMIN, adminMemberId));
    }
    public PolicySyncResult.JobSummary runScheduledSync() {
        var job = createJobWithDetectedYear(PolicySyncTriggerType.SCHEDULED, null);
        if (job.getStatus() == PolicySyncJobStatus.RUNNING) {
            runExistingJob(required(job.getId()));
            var summary = transactionTemplate.execute(status -> PolicySyncResult.JobSummary.from(findJob(required(job.getId()))));
            if (summary == null) throw new IllegalStateException("Transaction did not return a policy sync job summary");
            return summary;
        }
        return PolicySyncResult.JobSummary.from(job);
    }
    public void runExistingJob(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId");
        String targetYear = transactionTemplate.execute(status -> findJob(jobId).getTargetYear());
        if (targetYear == null) throw new IllegalStateException("Transaction did not return a policy sync target year");
        try {
            var unique = new LinkedHashMap<SourceIdentity, NongupEzPolicyListItem>();
            for (var item : sourceClient.fetchPrograms(targetYear)) unique.putIfAbsent(new SourceIdentity(item.getExternalId(), item.getSourceYear()), item);
            var listItems = new ArrayList<>(unique.values());
            var currentIdentities = new LinkedHashSet<>(unique.keySet());
            int detailSuccess = 0, detailFailure = 0;
            var detailFieldsByIdentity = new LinkedHashMap<SourceIdentity, DetailFields>();
            for (var item : listItems) {
                var identity = new SourceIdentity(item.getExternalId(), item.getSourceYear());
                DetailFields detailFields;
                try {
                    var detail = sourceClient.fetchDetail(item.getExternalId(), item.getSourceYear());
                    var tags = tagExtractor.extract(detail);
                    String periodLabel = cardTextGenerator.periodLabel(detail.getApplyStartsOn(), detail.getApplyEndsOn(), null);
                    var bodyParts = new ArrayList<String>();
                    for (String text : Arrays.asList(detail.getPurpose(), detail.getSummary(), detail.getEligibility(), detail.getBenefit())) if (text != null) bodyParts.add(text);
                    String body = String.join("\n\n", bodyParts);
                    if (trim(body).isEmpty()) body = item.getSummary() == null ? item.getTitle() : item.getSummary();
                    detailFields = new DetailFields(body, detail.getPurpose(), detail.getEligibility(), cardTextGenerator.eligibilitySummary(detail.getEligibility()),
                        detail.getBenefit(), cardTextGenerator.benefitSummary(detail.getBenefit()), detail.getApplyStartsOn(), detail.getApplyEndsOn(), periodLabel,
                        periodLabel.equals(UNKNOWN_PERIOD_LABEL) ? periodLabel : null, detail.getApplicationMethod(), detail.getRequiredDocuments(), detail.getSelectionCriteria(),
                        detail.getContacts().isEmpty() ? null : detail.getContacts().getFirst().getDepartmentName(), false, null,
                        textListJsonCodec.encode(tags.getTargetTags()), textListJsonCodec.encode(tags.getCropTags()), textListJsonCodec.encode(tags.getRegionTags()), detail.getRawJson());
                } catch (Exception e) { detailFields = null; }
                if (detailFields == null) detailFailure++; else detailSuccess++;
                detailFieldsByIdentity.put(identity, detailFields);
            }
            int synced = persistPrograms(targetYear, listItems, detailFieldsByIdentity, currentIdentities);
            succeedJob(jobId, listItems.size(), synced, detailSuccess, detailFailure);
        } catch (Exception e) { failJob(jobId, e); }
    }
    public PolicySyncResult.JobDetail getJob(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId");
        var result = transactionTemplate.execute(status -> PolicySyncResult.JobDetail.from(findJob(jobId)));
        if (result == null) throw new IllegalStateException("Transaction did not return a policy sync job detail");
        return result;
    }
    private String detailUrl(String externalId, String year) { return "https://www.nongupez.go.kr/nsm/bizAply/wholeBiz/wholeBizDtls?afbzCd=" + externalId + "&bizYr=" + year; }
    private PolicySyncJob createJobWithDetectedYear(PolicySyncTriggerType triggerType, UUID createdByMemberId) {
        try {
            String targetYear = sourceClient.detectLatestYear();
            var job = transactionTemplate.execute(status -> policySyncJobRepository.save(new PolicySyncJob(PolicySource.NONGUP_EZ, targetYear, triggerType, createdByMemberId)));
            if (job == null) throw new IllegalStateException("Transaction did not return a policy sync job");
            return job;
        } catch (Exception e) {
            var failed = new PolicySyncJob(PolicySource.NONGUP_EZ, Integer.toString(Year.now(clock).getValue()), triggerType, createdByMemberId);
            failed.fail(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            var result = transactionTemplate.execute(status -> policySyncJobRepository.save(failed));
            if (result == null) throw new IllegalStateException("Transaction did not return a failed policy sync job");
            return result;
        }
    }
    private int persistPrograms(String sourceYear, List<NongupEzPolicyListItem> listItems, Map<SourceIdentity, DetailFields> detailFieldsByIdentity, Set<SourceIdentity> currentIdentities) {
        Integer result = transactionTemplate.execute(status -> {
            var existingPrograms = policyProgramRepository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, sourceYear);
            var existingByIdentity = new LinkedHashMap<SourceIdentity, PolicyProgram>();
            for (var program : existingPrograms) existingByIdentity.put(new SourceIdentity(program.getExternalId(), program.getSourceYear()), program);
            int synced = 0;
            for (var item : listItems) {
                var identity = new SourceIdentity(item.getExternalId(), item.getSourceYear());
                if (upsertProgram(item, detailFieldsByIdentity.get(identity), existingByIdentity.get(identity))) synced++;
            }
            return synced + deactivateMissingSourcePolicies(currentIdentities, existingPrograms);
        });
        return result == null ? 0 : result;
    }
    private boolean upsertProgram(NongupEzPolicyListItem item, DetailFields fields, PolicyProgram existing) {
        boolean isNew = existing == null;
        var program = existing != null ? existing : new PolicyProgram(item.getTitle(), item.getSummary() == null ? item.getTitle() : item.getSummary(), DEFAULT_REGION, null);
        boolean changed = isNew;
        changed = program.applyListFields(PolicySource.NONGUP_EZ, item.getExternalId(), item.getSourceYear(), item.getTitle(), item.getSummary(), DEFAULT_REGION,
            detailUrl(item.getExternalId(), item.getSourceYear()), item.getAgencyName()) || changed;
        if (fields == null) {
            if (isNew || !program.getDetailSynced()) changed = program.markDetailSyncFailed(item.getRawJson()) || changed;
        } else changed = program.applyDetailFields(fields.body(), fields.purpose(), fields.eligibilityOriginal(), fields.eligibilitySummary(), fields.benefitOriginal(), fields.benefitSummary(), fields.applyStartsOn(), fields.applyEndsOn(), fields.applicationPeriodLabel(), fields.applicationPeriodNotice(), fields.applicationMethod(), fields.requiredDocuments(), fields.selectionCriteria(), fields.departmentName(), fields.onlineApplyAvailable(), fields.applicationUrl(), fields.targetTagsJson(), fields.cropTagsJson(), fields.regionTagsJson(), fields.rawPayload(), true) || changed;
        if (changed) policyProgramRepository.save(program);
        return changed;
    }
    private int deactivateMissingSourcePolicies(Set<SourceIdentity> currentIdentities, Collection<PolicyProgram> existingPrograms) {
        int changedCount = 0;
        for (var program : existingPrograms) {
            if (currentIdentities.contains(new SourceIdentity(program.getExternalId(), program.getSourceYear())) || !program.getRecommendable()) continue;
            boolean changed = program.applyDetailFields(program.getBody(), program.getPurpose(), program.getEligibilityOriginal(), program.getEligibilitySummary(), program.getBenefitOriginal(), program.getBenefitSummary(), program.getApplyStartsOn(), program.getApplyEndsOn(), program.getApplicationPeriodLabel(), program.getApplicationPeriodNotice(), program.getApplicationMethod(), program.getRequiredDocuments(), program.getSelectionCriteria(), program.getDepartmentName(), program.getOnlineApplyAvailable(), program.getApplicationUrl(), program.getTargetTagsJson(), program.getCropTagsJson(), program.getRegionTagsJson(), program.getRawPayload(), false);
            if (changed) { policyProgramRepository.save(program); changedCount++; }
        }
        return changedCount;
    }
    private void succeedJob(UUID jobId, int totalCount, int syncedCount, int detailSuccessCount, int detailFailureCount) {
        transactionTemplate.executeWithoutResult(status -> findJob(jobId).succeed(totalCount, syncedCount, detailSuccessCount, detailFailureCount));
    }
    private void failJob(UUID jobId, Exception e) {
        transactionTemplate.executeWithoutResult(status -> findJob(jobId).fail(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }
    private PolicySyncJob findJob(UUID jobId) {
        return policySyncJobRepository.findById(jobId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, jobId));
    }
    private static <T> T required(T value) { if (value == null) throw new IllegalArgumentException("Required value was null."); return value; }
    private record DetailFields(String body, String purpose, String eligibilityOriginal, String eligibilitySummary, String benefitOriginal,
        String benefitSummary, LocalDate applyStartsOn, LocalDate applyEndsOn, String applicationPeriodLabel, String applicationPeriodNotice,
        String applicationMethod, String requiredDocuments, String selectionCriteria, String departmentName, boolean onlineApplyAvailable,
        String applicationUrl, String targetTagsJson, String cropTagsJson, String regionTagsJson, String rawPayload) {}
    private record SourceIdentity(String externalId, String sourceYear) {}
    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
