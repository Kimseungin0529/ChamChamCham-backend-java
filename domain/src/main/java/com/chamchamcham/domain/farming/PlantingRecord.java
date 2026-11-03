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
@Table(name = "planting_record")
public class PlantingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @Enumerated(EnumType.STRING)
    @Column(name = "planting_method", nullable = false, length = 32)
    private PlantingMethod plantingMethod;

    @Column(name = "seed_amount", precision = 18, scale = 4)
    private BigDecimal seedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "seed_amount_unit", length = 16)
    private SeedAmountUnit seedAmountUnit;

    @Column(name = "seedling_count")
    private Integer seedlingCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "seedling_unit", length = 16)
    private SeedlingUnit seedlingUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "propagation_method", length = 32)
    private PropagationMethod propagationMethod;

    protected PlantingRecord() {}

    public PlantingRecord(UUID id, FarmingRecord record, PlantingMethod plantingMethod, BigDecimal seedAmount, SeedAmountUnit seedAmountUnit, Integer seedlingCount, SeedlingUnit seedlingUnit, PropagationMethod propagationMethod) {
        this.id = id;
        this.record = Objects.requireNonNull(record, "record");
        this.plantingMethod = Objects.requireNonNull(plantingMethod, "plantingMethod");
        this.seedAmount = seedAmount;
        this.seedAmountUnit = seedAmountUnit;
        this.seedlingCount = seedlingCount;
        this.seedlingUnit = seedlingUnit;
        this.propagationMethod = propagationMethod;
    }

    public PlantingRecord(FarmingRecord record, PlantingMethod plantingMethod) {
        this(null, record, plantingMethod, null, null, null, null, null);
    }

    public PlantingRecord(FarmingRecord record, PlantingMethod plantingMethod, BigDecimal seedAmount, SeedAmountUnit seedAmountUnit, Integer seedlingCount, SeedlingUnit seedlingUnit, PropagationMethod propagationMethod) {
        this(null, record, plantingMethod, seedAmount, seedAmountUnit, seedlingCount, seedlingUnit, propagationMethod);
    }

    public UUID getId() { return id; }

    public FarmingRecord getRecord() { return record; }

    public PlantingMethod getPlantingMethod() { return plantingMethod; }

    public BigDecimal getSeedAmount() { return seedAmount; }

    public SeedAmountUnit getSeedAmountUnit() { return seedAmountUnit; }

    public Integer getSeedlingCount() { return seedlingCount; }

    public SeedlingUnit getSeedlingUnit() { return seedlingUnit; }

    public PropagationMethod getPropagationMethod() { return propagationMethod; }
}
