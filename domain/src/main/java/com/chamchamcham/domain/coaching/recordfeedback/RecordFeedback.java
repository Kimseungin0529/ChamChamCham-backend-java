package com.chamchamcham.domain.coaching.recordfeedback;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.member.Member;
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
        name = "record_feedback",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_record_feedback_record_revision",
                columnNames = {"record_id", "source_revision"})
        })
public class RecordFeedback extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @Column(name = "source_revision", nullable = false)
    private long sourceRevision;

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

    protected RecordFeedback() {}

    public RecordFeedback(
            @Nullable UUID id,
            Member member,
            FarmingRecord record,
            RecordFeedbackStatus status,
            long sourceRevision,
            @Nullable Map<String, Object> inputSnapshot,
            List<Map<String, Object>> citations,
            @Nullable String auditStatus,
            List<String> auditWarnings,
            @Nullable String failureCode,
            @Nullable String modelName,
            @Nullable String embeddingModel
    ) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(citations, "citations");
        Objects.requireNonNull(auditWarnings, "auditWarnings");
        if (sourceRevision <= 0) throw new IllegalArgumentException("sourceRevision must be positive");
        this.id = id;
        this.member = member;
        this.record = record;
        this.status = status;
        this.sourceRevision = sourceRevision;
        this.inputSnapshot = inputSnapshot;
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
    public FarmingRecord getRecord() { return record; }
    public RecordFeedbackStatus getStatus() { return status; }
    public long getSourceRevision() { return sourceRevision; }
    @Nullable
    public Map<String, Object> getInputSnapshot() { return inputSnapshot; }
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

    public RecordFeedback(Member member, FarmingRecord record, RecordFeedbackStatus status, long sourceRevision) {
        this(null, member, record, status, sourceRevision, null, List.of(), null, List.of(), null, null, null);
    }

    public RecordFeedback(@Nullable UUID id, Member member, FarmingRecord record, RecordFeedbackStatus status, long sourceRevision) {
        this(id, member, record, status, sourceRevision, null, List.of(), null, List.of(), null, null, null);
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecordFeedbackStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> inputSnapshot;

    @Column(name = "failure_code", length = 128)
    private String failureCode;

    @Column(name = "good_point_basis", length = 255)
    private String goodPointBasis;

    @Column(name = "good_point_text", length = 255)
    private String goodPointText;

    @OneToMany(mappedBy = "recordFeedback", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<RecordFeedbackNextAction> nextActionRows = new ArrayList<>();

    @Nullable
    public String getGoodPointBasis() { return goodPointBasis; }
    @Nullable
    public String getGoodPointText() { return goodPointText; }
    public List<RecordFeedbackNextAction> nextActions() { return new ArrayList<>(nextActionRows); }

    public void attachInputSnapshot(Map<String, Object> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (status != RecordFeedbackStatus.PENDING) throw new IllegalStateException("input snapshot can only be attached while pending");
        inputSnapshot = snapshot;
    }

    public void markFailed(String code) {
        Objects.requireNonNull(code, "code");
        if (status != RecordFeedbackStatus.PENDING) throw new IllegalStateException("only pending feedback can fail");
        status = RecordFeedbackStatus.FAILED;
        failureCode = code;
    }

    public void markReady(String goodPointBasis, String goodPointText, List<RecordFeedbackNextActionDraft> nextActions,
                          List<Map<String, Object>> citations, String auditStatus, List<String> auditWarnings,
                          String modelName, String embeddingModel) {
        Objects.requireNonNull(goodPointBasis, "goodPointBasis");
        Objects.requireNonNull(goodPointText, "goodPointText");
        Objects.requireNonNull(nextActions, "nextActions");
        Objects.requireNonNull(citations, "citations");
        Objects.requireNonNull(auditStatus, "auditStatus");
        Objects.requireNonNull(auditWarnings, "auditWarnings");
        Objects.requireNonNull(modelName, "modelName");
        Objects.requireNonNull(embeddingModel, "embeddingModel");
        if (status != RecordFeedbackStatus.PENDING) throw new IllegalStateException("only pending feedback can become ready");
        if (isBlank(goodPointBasis)) throw new IllegalArgumentException("good point basis is required");
        if (isBlank(goodPointText)) throw new IllegalArgumentException("good point text is required");
        if (nextActions.size() < 2 || nextActions.size() > 3) throw new IllegalArgumentException("next action count must be 2 or 3");
        if (nextActions.stream().anyMatch(action -> isBlank(action.basis()) || isBlank(action.text()))) {
            throw new IllegalArgumentException("next action basis and text are required");
        }
        this.goodPointBasis = goodPointBasis;
        this.goodPointText = goodPointText;
        nextActionRows.clear();
        for (int i = 0; i < nextActions.size(); i++) {
            RecordFeedbackNextActionDraft action = nextActions.get(i);
            nextActionRows.add(new RecordFeedbackNextAction(this, i, action.due(), action.category(), action.basis(), action.text()));
        }
        this.citations = citations;
        this.auditStatus = auditStatus;
        this.auditWarnings = auditWarnings;
        this.modelName = modelName;
        this.embeddingModel = embeddingModel;
        status = RecordFeedbackStatus.READY;
        failureCode = null;
    }

    public void retry() {
        if (status != RecordFeedbackStatus.FAILED) throw new IllegalStateException("only failed feedback can retry");
        status = RecordFeedbackStatus.PENDING;
        failureCode = null;
        inputSnapshot = null;
    }

    public void markStale() {
        if (status == RecordFeedbackStatus.STALE) return;
        if (status != RecordFeedbackStatus.PENDING && status != RecordFeedbackStatus.READY) {
            throw new IllegalStateException("only pending or ready feedback can become stale");
        }
        status = RecordFeedbackStatus.STALE;
    }

    public static RecordFeedback pending(Member member, FarmingRecord record, long sourceRevision) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(record, "record");
        return new RecordFeedback(member, record, RecordFeedbackStatus.PENDING, sourceRevision);
    }
    private static boolean isBlank(String value) {
        return value.chars().allMatch(character -> Character.isWhitespace(character) || Character.isSpaceChar(character));
    }
}
