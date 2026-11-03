package com.chamchamcham.domain.coaching.recordfeedback;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

@Entity
@Table(
        name = "record_feedback_next_action",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_record_feedback_next_action_order",
                columnNames = {"record_feedback_id", "display_order"})
        })
public class RecordFeedbackNextAction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_feedback_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecordFeedback recordFeedback;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecordFeedbackActionDue due;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecordFeedbackActionCategory category;

    @Column(nullable = false, length = 255)
    private String basis;

    @Column(nullable = false, length = 255)
    private String text;

    protected RecordFeedbackNextAction() {}

    public RecordFeedbackNextAction(
            @Nullable UUID id,
            RecordFeedback recordFeedback,
            int displayOrder,
            RecordFeedbackActionDue due,
            RecordFeedbackActionCategory category,
            String basis,
            String text
    ) {
        Objects.requireNonNull(recordFeedback, "recordFeedback");
        Objects.requireNonNull(due, "due");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(text, "text");
        this.id = id;
        this.recordFeedback = recordFeedback;
        this.displayOrder = displayOrder;
        this.due = due;
        this.category = category;
        this.basis = basis;
        this.text = text;
    }

    @Nullable
    public UUID getId() { return id; }
    public RecordFeedback getRecordFeedback() { return recordFeedback; }
    public int getDisplayOrder() { return displayOrder; }
    public RecordFeedbackActionDue getDue() { return due; }
    public RecordFeedbackActionCategory getCategory() { return category; }
    public String getBasis() { return basis; }
    public String getText() { return text; }

    public RecordFeedbackNextAction(
            RecordFeedback recordFeedback,
            int displayOrder,
            RecordFeedbackActionDue due,
            RecordFeedbackActionCategory category,
            String basis,
            String text
    ) {
        this(null, recordFeedback, displayOrder, due, category, basis, text);
    }

}
