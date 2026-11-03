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
@Table(name = "watering_record")
public class WateringRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @Enumerated(EnumType.STRING)
    @Column(name = "irrigation_amount", length = 16)
    private IrrigationAmount irrigationAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "irrigation_method", length = 16)
    private IrrigationMethod irrigationMethod;

    protected WateringRecord() {}

    public WateringRecord(UUID id, FarmingRecord record, IrrigationAmount irrigationAmount, IrrigationMethod irrigationMethod) {
        this.id = id;
        this.record = Objects.requireNonNull(record, "record");
        this.irrigationAmount = irrigationAmount;
        this.irrigationMethod = irrigationMethod;
    }

    public WateringRecord(FarmingRecord record) {
        this(null, record, null, null);
    }

    public WateringRecord(FarmingRecord record, IrrigationAmount irrigationAmount, IrrigationMethod irrigationMethod) {
        this(null, record, irrigationAmount, irrigationMethod);
    }

    public UUID getId() { return id; }

    public FarmingRecord getRecord() { return record; }

    public IrrigationAmount getIrrigationAmount() { return irrigationAmount; }

    public IrrigationMethod getIrrigationMethod() { return irrigationMethod; }
}
