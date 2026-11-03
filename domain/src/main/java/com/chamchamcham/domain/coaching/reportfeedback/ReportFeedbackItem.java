package com.chamchamcham.domain.coaching.reportfeedback;

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
        name = "report_feedback_item",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_report_feedback_item_order",
                columnNames = {"report_feedback_id", "display_order"})
        })
public class ReportFeedbackItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_feedback_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ReportFeedback reportFeedback;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportFeedbackItemSection section;

    @Column(nullable = false, columnDefinition = "text")
    private String basis;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    protected ReportFeedbackItem() {}

    public ReportFeedbackItem(
            @Nullable UUID id,
            ReportFeedback reportFeedback,
            int displayOrder,
            ReportFeedbackItemSection section,
            String basis,
            String text
    ) {
        Objects.requireNonNull(reportFeedback, "reportFeedback");
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(text, "text");
        this.id = id;
        this.reportFeedback = reportFeedback;
        this.displayOrder = displayOrder;
        this.section = section;
        this.basis = basis;
        this.text = text;
    }

    @Nullable
    public UUID getId() { return id; }
    public ReportFeedback getReportFeedback() { return reportFeedback; }
    public int getDisplayOrder() { return displayOrder; }
    public ReportFeedbackItemSection getSection() { return section; }
    public String getBasis() { return basis; }
    public String getText() { return text; }

    public ReportFeedbackItem(
            ReportFeedback reportFeedback,
            int displayOrder,
            ReportFeedbackItemSection section,
            String basis,
            String text
    ) {
        this(null, reportFeedback, displayOrder, section, basis, text);
    }

}
