package com.chamchamcham.domain.coaching.reportfeedback;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.FarmingCycleReport;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.springframework.lang.Nullable;

@Entity
@Table(
        name = "report_feedback",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_report_feedback_report_work_type",
                columnNames = {"report_id", "work_type"})
        })
public class ReportFeedback extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingCycleReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 32)
    private WorkType workType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> citations;

    @Column(name = "audit_status", length = 32)
    private String auditStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_warnings", nullable = false, columnDefinition = "jsonb")
    private List<String> auditWarnings;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "embedding_model", length = 128)
    private String embeddingModel;

    protected ReportFeedback() {}

    public ReportFeedback(
            @Nullable UUID id,
            Member member,
            FarmingCycleReport report,
            WorkType workType,
            ReportFeedbackStatus status,
            @Nullable Map<String, Object> inputSnapshot,
            @Nullable String sourceFingerprint,
            List<Map<String, Object>> citations,
            @Nullable String auditStatus,
            List<String> auditWarnings,
            @Nullable String failureCode,
            @Nullable String modelName,
            @Nullable String embeddingModel
    ) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(citations, "citations");
        Objects.requireNonNull(auditWarnings, "auditWarnings");
        this.id = id;
        this.member = member;
        this.report = report;
        this.workType = workType;
        this.status = status;
        this.inputSnapshot = inputSnapshot;
        this.sourceFingerprint = sourceFingerprint;
        this.citations = citations;
        this.auditStatus = auditStatus;
        this.auditWarnings = auditWarnings;
        this.failureCode = failureCode;
        this.modelName = modelName;
        this.embeddingModel = embeddingModel;
    }

    @Nullable
    public UUID getId() { return id; }
    public Member getMember() { return member; }
    public FarmingCycleReport getReport() { return report; }
    public WorkType getWorkType() { return workType; }
    public ReportFeedbackStatus getStatus() { return status; }
    @Nullable
    public Map<String, Object> getInputSnapshot() { return inputSnapshot; }
    @Nullable
    public String getSourceFingerprint() { return sourceFingerprint; }
    public List<Map<String, Object>> getCitations() { return citations; }
    public void setCitations(List<Map<String, Object>> citations) { this.citations = Objects.requireNonNull(citations, "citations"); }
    @Nullable
    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(@Nullable String auditStatus) { this.auditStatus = auditStatus; }
    public List<String> getAuditWarnings() { return auditWarnings; }
    public void setAuditWarnings(List<String> auditWarnings) { this.auditWarnings = Objects.requireNonNull(auditWarnings, "auditWarnings"); }
    @Nullable
    public String getFailureCode() { return failureCode; }
    @Nullable
    public String getModelName() { return modelName; }
    public void setModelName(@Nullable String modelName) { this.modelName = modelName; }
    @Nullable
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(@Nullable String embeddingModel) { this.embeddingModel = embeddingModel; }

    public ReportFeedback(Member member, FarmingCycleReport report, WorkType workType, ReportFeedbackStatus status) {
        this(null, member, report, workType, status, null, null, List.of(), null, List.of(), null, null, null);
    }

    public ReportFeedback(
            @Nullable UUID id,
            Member member,
            FarmingCycleReport report,
            WorkType workType,
            ReportFeedbackStatus status
    ) {
        this(id, member, report, workType, status, null, null, List.of(), null, List.of(), null, null, null);
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportFeedbackStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> inputSnapshot;

    @Column(name = "source_fingerprint", length = 64)
    private String sourceFingerprint;

    @Column(name = "failure_code", length = 128)
    private String failureCode;

    @Column(columnDefinition = "text")
    private String summary;

    @OneToMany(mappedBy = "reportFeedback", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<ReportFeedbackItem> itemRows = new ArrayList<>();

    @Nullable
    public String getSummary() { return summary; }
    public List<ReportFeedbackItem> items() { return new ArrayList<>(itemRows); }

    public void attachInputSnapshot(Map<String, Object> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (status != ReportFeedbackStatus.PENDING) throw new IllegalStateException("input snapshot can only be attached while pending");
        inputSnapshot = snapshot;
    }

    public void markFailed(String code) {
        Objects.requireNonNull(code, "code");
        if (status != ReportFeedbackStatus.PENDING) throw new IllegalStateException("only pending feedback can fail");
        status = ReportFeedbackStatus.FAILED;
        failureCode = code;
    }

    public void markStale() {
        if (status == ReportFeedbackStatus.STALE) return;
        status = ReportFeedbackStatus.STALE;
        failureCode = null;
        inputSnapshot = null;
    }

    public void backfillSourceFingerprint(String sourceFingerprint) {
        Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        requireValidFingerprint(sourceFingerprint);
        if (this.sourceFingerprint != null) return;
        if (status != ReportFeedbackStatus.PENDING && status != ReportFeedbackStatus.READY && status != ReportFeedbackStatus.FAILED) {
            throw new IllegalStateException("only non-stale legacy feedback can backfill its source fingerprint");
        }
        this.sourceFingerprint = sourceFingerprint;
    }

    public void retry(String sourceFingerprint) {
        Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        if (status != ReportFeedbackStatus.FAILED && status != ReportFeedbackStatus.STALE) {
            throw new IllegalStateException("only failed or stale feedback can retry");
        }
        requireValidFingerprint(sourceFingerprint);
        status = ReportFeedbackStatus.PENDING;
        this.sourceFingerprint = sourceFingerprint;
        failureCode = null;
        inputSnapshot = null;
    }

    public void markReady(String summary, List<ReportFeedbackItemDraft> items, List<Map<String, Object>> citations,
                          String auditStatus, List<String> auditWarnings, String modelName, String embeddingModel) {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(citations, "citations");
        Objects.requireNonNull(auditStatus, "auditStatus");
        Objects.requireNonNull(auditWarnings, "auditWarnings");
        Objects.requireNonNull(modelName, "modelName");
        Objects.requireNonNull(embeddingModel, "embeddingModel");
        if (status != ReportFeedbackStatus.PENDING) throw new IllegalStateException("only pending feedback can become ready");
        if (isBlank(summary)) throw new IllegalArgumentException("summary is required");
        if (items.stream().anyMatch(item -> isBlank(item.basis()) || isBlank(item.text()))) {
            throw new IllegalArgumentException("feedback item basis and text are required");
        }
        this.summary = summary;
        itemRows.clear();
        for (int i = 0; i < items.size(); i++) {
            ReportFeedbackItemDraft item = items.get(i);
            itemRows.add(new ReportFeedbackItem(this, i, item.section(), item.basis(), item.text()));
        }
        this.citations = citations;
        this.auditStatus = auditStatus;
        this.auditWarnings = auditWarnings;
        this.modelName = modelName;
        this.embeddingModel = embeddingModel;
        status = ReportFeedbackStatus.READY;
        failureCode = null;
    }

    public static ReportFeedback pending(Member member, FarmingCycleReport report, WorkType workType, String sourceFingerprint) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        requireValidFingerprint(sourceFingerprint);
        return new ReportFeedback(null, member, report, workType, ReportFeedbackStatus.PENDING, null,
                sourceFingerprint, List.of(), null, List.of(), null, null, null);
    }

    public static ReportFeedback stalePlaceholder(Member member, FarmingCycleReport report, WorkType workType) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(workType, "workType");
        return new ReportFeedback(member, report, workType, ReportFeedbackStatus.STALE);
    }

    private static void requireValidFingerprint(@Nullable String sourceFingerprint) {
        if (!sourceFingerprint.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("source fingerprint must be lowercase SHA-256 hex");
        }
    }
    private static boolean isBlank(String value) {
        return value.chars().allMatch(character -> Character.isWhitespace(character) || Character.isSpaceChar(character));
    }
}
