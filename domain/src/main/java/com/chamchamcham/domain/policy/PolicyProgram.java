package com.chamchamcham.domain.policy;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.member.ManagementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "policy_program", uniqueConstraints = @UniqueConstraint(
    name = "uk_policy_program_source_external_year", columnNames = {"source", "external_id", "source_year"}
))
public class PolicyProgram extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, length = 128)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_management_type", length = 32)
    private ManagementType targetManagementType;

    @Column(name = "apply_starts_on")
    private LocalDate applyStartsOn;

    @Column(name = "apply_ends_on")
    private LocalDate applyEndsOn;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicySource source;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "source_year", nullable = false, length = 4)
    private String sourceYear;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "agency_name", nullable = false, length = 255)
    private String agencyName;

    @Column(name = "department_name", length = 255)
    private String departmentName;

    @Column(name = "online_apply_available", nullable = false)
    private boolean onlineApplyAvailable;

    @Column(name = "application_url", length = 2048)
    private String applicationUrl;

    @Column(name = "application_period_label", nullable = false, length = 19)
    private String applicationPeriodLabel;

    @Column(name = "application_period_notice", length = 255)
    private String applicationPeriodNotice;

    @Column(name = "eligibility_original", columnDefinition = "text")
    private String eligibilityOriginal;

    @Column(name = "eligibility_summary", nullable = false, length = 19)
    private String eligibilitySummary;

    @Column(name = "benefit_original", columnDefinition = "text")
    private String benefitOriginal;

    @Column(name = "benefit_summary", nullable = false, length = 19)
    private String benefitSummary;

    @Column(name = "purpose", columnDefinition = "text")
    private String purpose;

    @Column(name = "application_method", columnDefinition = "text")
    private String applicationMethod;

    @Column(name = "required_documents", columnDefinition = "text")
    private String requiredDocuments;

    @Column(name = "selection_criteria", columnDefinition = "text")
    private String selectionCriteria;

    @Column(name = "detail_synced", nullable = false)
    private boolean detailSynced;

    @Column(nullable = false)
    private boolean recommendable;

    @Column(name = "target_tags_json", nullable = false, columnDefinition = "text")
    private String targetTagsJson;

    @Column(name = "crop_tags_json", nullable = false, columnDefinition = "text")
    private String cropTagsJson;

    @Column(name = "region_tags_json", nullable = false, columnDefinition = "text")
    private String regionTagsJson;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    protected PolicyProgram() {
    }

    public PolicyProgram(
        String title,
        String body,
        String region,
        @Nullable ManagementType targetManagementType
    ) {
        this(null, title, body, region, targetManagementType, null, null, null,
            PolicySource.NONGUP_EZ, "", "", null, "", null, false, null,
            "접수기관문의", null, null, "상세 자격 확인", null, "상세 지원 확인", null, null,
            null, null, false, false, "[]", "[]", "[]", "{}");
    }

    public PolicyProgram(
        @Nullable UUID id,
        String title,
        String body,
        String region,
        @Nullable ManagementType targetManagementType
    ) {
        this(id, title, body, region, targetManagementType, null, null, null,
            PolicySource.NONGUP_EZ, "", "", null, "", null, false, null,
            "접수기관문의", null, null, "상세 자격 확인", null, "상세 지원 확인", null, null,
            null, null, false, false, "[]", "[]", "[]", "{}");
    }

    public PolicyProgram(
        @Nullable UUID id,
        String title,
        String body,
        String region,
        @Nullable ManagementType targetManagementType,
        @Nullable LocalDate applyStartsOn,
        @Nullable LocalDate applyEndsOn,
        @Nullable String sourceUrl,
        PolicySource source,
        String externalId,
        String sourceYear,
        @Nullable String summary,
        String agencyName,
        @Nullable String departmentName,
        boolean onlineApplyAvailable,
        @Nullable String applicationUrl,
        String applicationPeriodLabel,
        @Nullable String applicationPeriodNotice,
        @Nullable String eligibilityOriginal,
        String eligibilitySummary,
        @Nullable String benefitOriginal,
        String benefitSummary,
        @Nullable String purpose,
        @Nullable String applicationMethod,
        @Nullable String requiredDocuments,
        @Nullable String selectionCriteria,
        boolean detailSynced,
        boolean recommendable,
        String targetTagsJson,
        String cropTagsJson,
        String regionTagsJson,
        String rawPayload
    ) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "title");
        this.body = Objects.requireNonNull(body, "body");
        this.region = Objects.requireNonNull(region, "region");
        this.targetManagementType = targetManagementType;
        this.applyStartsOn = applyStartsOn;
        this.applyEndsOn = applyEndsOn;
        this.sourceUrl = sourceUrl;
        this.source = Objects.requireNonNull(source, "source");
        this.externalId = Objects.requireNonNull(externalId, "externalId");
        this.sourceYear = Objects.requireNonNull(sourceYear, "sourceYear");
        this.summary = summary;
        this.agencyName = Objects.requireNonNull(agencyName, "agencyName");
        this.departmentName = departmentName;
        this.onlineApplyAvailable = onlineApplyAvailable;
        this.applicationUrl = applicationUrl;
        this.applicationPeriodLabel = Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
        this.applicationPeriodNotice = applicationPeriodNotice;
        this.eligibilityOriginal = eligibilityOriginal;
        this.eligibilitySummary = Objects.requireNonNull(eligibilitySummary, "eligibilitySummary");
        this.benefitOriginal = benefitOriginal;
        this.benefitSummary = Objects.requireNonNull(benefitSummary, "benefitSummary");
        this.purpose = purpose;
        this.applicationMethod = applicationMethod;
        this.requiredDocuments = requiredDocuments;
        this.selectionCriteria = selectionCriteria;
        this.detailSynced = detailSynced;
        this.recommendable = recommendable;
        this.targetTagsJson = Objects.requireNonNull(targetTagsJson, "targetTagsJson");
        this.cropTagsJson = Objects.requireNonNull(cropTagsJson, "cropTagsJson");
        this.regionTagsJson = Objects.requireNonNull(regionTagsJson, "regionTagsJson");
        this.rawPayload = Objects.requireNonNull(rawPayload, "rawPayload");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = Objects.requireNonNull(body, "body");
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = Objects.requireNonNull(region, "region");
    }

    @Nullable
    public ManagementType getTargetManagementType() {
        return targetManagementType;
    }

    public void setTargetManagementType(@Nullable ManagementType targetManagementType) {
        this.targetManagementType = targetManagementType;
    }

    @Nullable
    public LocalDate getApplyStartsOn() {
        return applyStartsOn;
    }

    public void setApplyStartsOn(@Nullable LocalDate applyStartsOn) {
        this.applyStartsOn = applyStartsOn;
    }

    @Nullable
    public LocalDate getApplyEndsOn() {
        return applyEndsOn;
    }

    public void setApplyEndsOn(@Nullable LocalDate applyEndsOn) {
        this.applyEndsOn = applyEndsOn;
    }

    @Nullable
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(@Nullable String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public PolicySource getSource() {
        return source;
    }

    public void setSource(PolicySource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = Objects.requireNonNull(externalId, "externalId");
    }

    public String getSourceYear() {
        return sourceYear;
    }

    public void setSourceYear(String sourceYear) {
        this.sourceYear = Objects.requireNonNull(sourceYear, "sourceYear");
    }

    @Nullable
    public String getSummary() {
        return summary;
    }

    public void setSummary(@Nullable String summary) {
        this.summary = summary;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = Objects.requireNonNull(agencyName, "agencyName");
    }

    @Nullable
    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(@Nullable String departmentName) {
        this.departmentName = departmentName;
    }

    public boolean getOnlineApplyAvailable() {
        return onlineApplyAvailable;
    }

    public void setOnlineApplyAvailable(boolean onlineApplyAvailable) {
        this.onlineApplyAvailable = onlineApplyAvailable;
    }

    @Nullable
    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(@Nullable String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public String getApplicationPeriodLabel() {
        return applicationPeriodLabel;
    }

    public void setApplicationPeriodLabel(String applicationPeriodLabel) {
        this.applicationPeriodLabel = Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
    }

    @Nullable
    public String getApplicationPeriodNotice() {
        return applicationPeriodNotice;
    }

    public void setApplicationPeriodNotice(@Nullable String applicationPeriodNotice) {
        this.applicationPeriodNotice = applicationPeriodNotice;
    }

    @Nullable
    public String getEligibilityOriginal() {
        return eligibilityOriginal;
    }

    public void setEligibilityOriginal(@Nullable String eligibilityOriginal) {
        this.eligibilityOriginal = eligibilityOriginal;
    }

    public String getEligibilitySummary() {
        return eligibilitySummary;
    }

    public void setEligibilitySummary(String eligibilitySummary) {
        this.eligibilitySummary = Objects.requireNonNull(eligibilitySummary, "eligibilitySummary");
    }

    @Nullable
    public String getBenefitOriginal() {
        return benefitOriginal;
    }

    public void setBenefitOriginal(@Nullable String benefitOriginal) {
        this.benefitOriginal = benefitOriginal;
    }

    public String getBenefitSummary() {
        return benefitSummary;
    }

    public void setBenefitSummary(String benefitSummary) {
        this.benefitSummary = Objects.requireNonNull(benefitSummary, "benefitSummary");
    }

    @Nullable
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(@Nullable String purpose) {
        this.purpose = purpose;
    }

    @Nullable
    public String getApplicationMethod() {
        return applicationMethod;
    }

    public void setApplicationMethod(@Nullable String applicationMethod) {
        this.applicationMethod = applicationMethod;
    }

    @Nullable
    public String getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(@Nullable String requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }

    @Nullable
    public String getSelectionCriteria() {
        return selectionCriteria;
    }

    public void setSelectionCriteria(@Nullable String selectionCriteria) {
        this.selectionCriteria = selectionCriteria;
    }

    public boolean getDetailSynced() {
        return detailSynced;
    }

    public void setDetailSynced(boolean detailSynced) {
        this.detailSynced = detailSynced;
    }

    public boolean getRecommendable() {
        return recommendable;
    }

    public void setRecommendable(boolean recommendable) {
        this.recommendable = recommendable;
    }

    public String getTargetTagsJson() {
        return targetTagsJson;
    }

    public void setTargetTagsJson(String targetTagsJson) {
        this.targetTagsJson = Objects.requireNonNull(targetTagsJson, "targetTagsJson");
    }

    public String getCropTagsJson() {
        return cropTagsJson;
    }

    public void setCropTagsJson(String cropTagsJson) {
        this.cropTagsJson = Objects.requireNonNull(cropTagsJson, "cropTagsJson");
    }

    public String getRegionTagsJson() {
        return regionTagsJson;
    }

    public void setRegionTagsJson(String regionTagsJson) {
        this.regionTagsJson = Objects.requireNonNull(regionTagsJson, "regionTagsJson");
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = Objects.requireNonNull(rawPayload, "rawPayload");
    }

    public boolean applyListFields(
        PolicySource source,
        String externalId,
        String sourceYear,
        String title,
        @Nullable String summary,
        String region,
        @Nullable String sourceUrl,
        String agencyName
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(sourceYear, "sourceYear");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(agencyName, "agencyName");
        boolean changed = false;
        if (!Objects.equals(this.source, source)) {
            this.source = source;
            changed = true;
        }
        if (!Objects.equals(this.externalId, externalId)) {
            this.externalId = externalId;
            changed = true;
        }
        if (!Objects.equals(this.sourceYear, sourceYear)) {
            this.sourceYear = sourceYear;
            changed = true;
        }
        if (!Objects.equals(this.title, title)) {
            this.title = title;
            changed = true;
        }
        if (!Objects.equals(this.summary, summary)) {
            this.summary = summary;
            changed = true;
        }
        if (!detailSynced && summary != null && !Objects.equals(this.body, summary)) {
            this.body = summary;
            changed = true;
        }
        if (!Objects.equals(this.region, region)) {
            this.region = region;
            changed = true;
        }
        if (!Objects.equals(this.sourceUrl, sourceUrl)) {
            this.sourceUrl = sourceUrl;
            changed = true;
        }
        if (!Objects.equals(this.agencyName, agencyName)) {
            this.agencyName = agencyName;
            changed = true;
        }
        return changed;
    }

    public boolean applyDetailFields(
        String body,
        @Nullable String purpose,
        @Nullable String eligibilityOriginal,
        String eligibilitySummary,
        @Nullable String benefitOriginal,
        String benefitSummary,
        @Nullable LocalDate applyStartsOn,
        @Nullable LocalDate applyEndsOn,
        String applicationPeriodLabel,
        @Nullable String applicationPeriodNotice,
        @Nullable String applicationMethod,
        @Nullable String requiredDocuments,
        @Nullable String selectionCriteria,
        @Nullable String departmentName,
        boolean onlineApplyAvailable,
        @Nullable String applicationUrl,
        String targetTagsJson,
        String cropTagsJson,
        String regionTagsJson,
        String rawPayload,
        boolean recommendable
    ) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(eligibilitySummary, "eligibilitySummary");
        Objects.requireNonNull(benefitSummary, "benefitSummary");
        Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
        Objects.requireNonNull(targetTagsJson, "targetTagsJson");
        Objects.requireNonNull(cropTagsJson, "cropTagsJson");
        Objects.requireNonNull(regionTagsJson, "regionTagsJson");
        Objects.requireNonNull(rawPayload, "rawPayload");
        if (eligibilitySummary.length() > 19 || benefitSummary.length() > 19 || applicationPeriodLabel.length() > 19) {
            throw new IllegalArgumentException("Failed requirement.");
        }
        boolean changed = false;
        if (!Objects.equals(this.body, body)) {
            this.body = body;
            changed = true;
        }
        if (!Objects.equals(this.purpose, purpose)) {
            this.purpose = purpose;
            changed = true;
        }
        if (!Objects.equals(this.eligibilityOriginal, eligibilityOriginal)) {
            this.eligibilityOriginal = eligibilityOriginal;
            changed = true;
        }
        if (!Objects.equals(this.eligibilitySummary, eligibilitySummary)) {
            this.eligibilitySummary = eligibilitySummary;
            changed = true;
        }
        if (!Objects.equals(this.benefitOriginal, benefitOriginal)) {
            this.benefitOriginal = benefitOriginal;
            changed = true;
        }
        if (!Objects.equals(this.benefitSummary, benefitSummary)) {
            this.benefitSummary = benefitSummary;
            changed = true;
        }
        if (!Objects.equals(this.applyStartsOn, applyStartsOn)) {
            this.applyStartsOn = applyStartsOn;
            changed = true;
        }
        if (!Objects.equals(this.applyEndsOn, applyEndsOn)) {
            this.applyEndsOn = applyEndsOn;
            changed = true;
        }
        if (!Objects.equals(this.applicationPeriodLabel, applicationPeriodLabel)) {
            this.applicationPeriodLabel = applicationPeriodLabel;
            changed = true;
        }
        if (!Objects.equals(this.applicationPeriodNotice, applicationPeriodNotice)) {
            this.applicationPeriodNotice = applicationPeriodNotice;
            changed = true;
        }
        if (!Objects.equals(this.applicationMethod, applicationMethod)) {
            this.applicationMethod = applicationMethod;
            changed = true;
        }
        if (!Objects.equals(this.requiredDocuments, requiredDocuments)) {
            this.requiredDocuments = requiredDocuments;
            changed = true;
        }
        if (!Objects.equals(this.selectionCriteria, selectionCriteria)) {
            this.selectionCriteria = selectionCriteria;
            changed = true;
        }
        if (!Objects.equals(this.departmentName, departmentName)) {
            this.departmentName = departmentName;
            changed = true;
        }
        if (this.onlineApplyAvailable != onlineApplyAvailable) {
            this.onlineApplyAvailable = onlineApplyAvailable;
            changed = true;
        }
        if (!Objects.equals(this.applicationUrl, applicationUrl)) {
            this.applicationUrl = applicationUrl;
            changed = true;
        }
        if (!Objects.equals(this.targetTagsJson, targetTagsJson)) {
            this.targetTagsJson = targetTagsJson;
            changed = true;
        }
        if (!Objects.equals(this.cropTagsJson, cropTagsJson)) {
            this.cropTagsJson = cropTagsJson;
            changed = true;
        }
        if (!Objects.equals(this.regionTagsJson, regionTagsJson)) {
            this.regionTagsJson = regionTagsJson;
            changed = true;
        }
        if (!Objects.equals(this.rawPayload, rawPayload)) {
            this.rawPayload = rawPayload;
            changed = true;
        }
        if (this.recommendable != recommendable) {
            this.recommendable = recommendable;
            changed = true;
        }
        if (!detailSynced) {
            detailSynced = true;
            changed = true;
        }
        return changed;
    }

    public boolean markDetailSyncFailed(String rawPayload) {
        Objects.requireNonNull(rawPayload, "rawPayload");
        boolean changed = false;
        if (detailSynced) {
            detailSynced = false;
            changed = true;
        }
        if (recommendable) {
            recommendable = false;
            changed = true;
        }
        if (!Objects.equals(this.rawPayload, rawPayload)) {
            this.rawPayload = rawPayload;
            changed = true;
        }
        return changed;
    }

    public boolean isOpenOn(LocalDate today) {
        Objects.requireNonNull(today, "today");
        return applyEndsOn == null || !applyEndsOn.isBefore(today);
    }
}
