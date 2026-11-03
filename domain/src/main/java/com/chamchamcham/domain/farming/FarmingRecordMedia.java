package com.chamchamcham.domain.farming;

import java.util.Objects;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.media.UploadedMedia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.UUID;

@Entity
@Table(
    name = "farming_record_media",
    uniqueConstraints = {
    @UniqueConstraint(name = "uk_farming_record_media_uploaded_media", columnNames = {"uploaded_media_id"})
    }
)
public class FarmingRecordMedia extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_media_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UploadedMedia uploadedMedia;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected FarmingRecordMedia() {}

    public FarmingRecordMedia(UUID id, FarmingRecord record, UploadedMedia uploadedMedia, int displayOrder) {
        this.id = id;
        this.record = Objects.requireNonNull(record, "record");
        this.uploadedMedia = Objects.requireNonNull(uploadedMedia, "uploadedMedia");
        this.displayOrder = displayOrder;
    }

    public FarmingRecordMedia(FarmingRecord record, UploadedMedia uploadedMedia, int displayOrder) {
        this(null, record, uploadedMedia, displayOrder);
    }

    public UUID getId() { return id; }

    public FarmingRecord getRecord() { return record; }

    public UploadedMedia getUploadedMedia() { return uploadedMedia; }

    public int getDisplayOrder() { return displayOrder; }
}
