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
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fertilizing_record")
public class FertilizingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @Column(name = "material_name", nullable = false, length = 255)
    private String materialName;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_unit", nullable = false, length = 16)
    private FertilizerAmountUnit amountUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_method", length = 16)
    private FertilizingMethod applicationMethod;

    protected FertilizingRecord() {}

    public FertilizingRecord(UUID id, FarmingRecord record, String materialName, BigDecimal amount, FertilizerAmountUnit amountUnit, FertilizingMethod applicationMethod) {
        this.id = id;
        this.record = Objects.requireNonNull(record, "record");
        this.materialName = Objects.requireNonNull(materialName, "materialName");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.amountUnit = Objects.requireNonNull(amountUnit, "amountUnit");
        this.applicationMethod = applicationMethod;
    }

    public FertilizingRecord(FarmingRecord record, String materialName, BigDecimal amount, FertilizerAmountUnit amountUnit) {
        this(null, record, materialName, amount, amountUnit, null);
    }

    public FertilizingRecord(FarmingRecord record, String materialName, BigDecimal amount, FertilizerAmountUnit amountUnit, FertilizingMethod applicationMethod) {
        this(null, record, materialName, amount, amountUnit, applicationMethod);
    }

    public UUID getId() { return id; }

    public FarmingRecord getRecord() { return record; }

    public String getMaterialName() { return materialName; }

    public BigDecimal getAmount() { return amount; }

    public FertilizerAmountUnit getAmountUnit() { return amountUnit; }

    public FertilizingMethod getApplicationMethod() { return applicationMethod; }
}
