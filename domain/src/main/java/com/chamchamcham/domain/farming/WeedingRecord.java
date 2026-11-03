package com.chamchamcham.domain.farming;

import java.util.Objects;
import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.UUID;

@Entity
@Table(name = "weeding_record")
public class WeedingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @Enumerated(EnumType.STRING)
    @Column(name = "weeding_method", length = 16)
    private WeedingMethod weedingMethod;

    protected WeedingRecord() {}

    public WeedingRecord(UUID id, FarmingRecord record, WeedingMethod weedingMethod) {
        this.id = id;
        this.record = Objects.requireNonNull(record, "record");
        this.weedingMethod = weedingMethod;
    }

    public WeedingRecord(FarmingRecord record) {
        this(null, record, null);
    }

    public WeedingRecord(FarmingRecord record, WeedingMethod weedingMethod) {
        this(null, record, weedingMethod);
    }

    public UUID getId() { return id; }

    public FarmingRecord getRecord() { return record; }

    public WeedingMethod getWeedingMethod() { return weedingMethod; }
}
