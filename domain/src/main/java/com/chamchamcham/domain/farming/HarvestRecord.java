package com.chamchamcham.domain.farming;

import java.util.Objects;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.CropUsePartCategory;
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
@Table(name = "harvest_record")
public class HarvestRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @Column(name = "harvest_amount", precision = 18, scale = 4)
    private BigDecimal harvestAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "medicinal_part", length = 32)
    private CropUsePartCategory medicinalPart;

    @Enumerated(EnumType.STRING)
    @Column(name = "harvest_source", nullable = false, length = 16)
    private HarvestSource harvestSource;

    @Column(name = "growth_period", nullable = false)
    private int growthPeriod;

    @Column(name = "is_last_harvest", nullable = false)
    private boolean isLastHarvest;

    protected HarvestRecord() {}

    public HarvestRecord(UUID id, FarmingRecord record, BigDecimal harvestAmount, CropUsePartCategory medicinalPart, HarvestSource harvestSource, int growthPeriod, boolean isLastHarvest) {
        this.id = id;
        this.record = Objects.requireNonNull(record, "record");
        this.harvestAmount = harvestAmount;
        this.medicinalPart = medicinalPart;
        this.harvestSource = Objects.requireNonNull(harvestSource, "harvestSource");
        this.growthPeriod = growthPeriod;
        this.isLastHarvest = isLastHarvest;
    }

    public HarvestRecord(FarmingRecord record, BigDecimal harvestAmount, int growthPeriod, boolean isLastHarvest) {
        this(null, record, harvestAmount, null, HarvestSource.CULTIVATED, growthPeriod, isLastHarvest);
    }

    public HarvestRecord(FarmingRecord record, BigDecimal harvestAmount, CropUsePartCategory medicinalPart, HarvestSource harvestSource, int growthPeriod, boolean isLastHarvest) {
        this(null, record, harvestAmount, medicinalPart, harvestSource, growthPeriod, isLastHarvest);
    }

    public UUID getId() { return id; }

    public FarmingRecord getRecord() { return record; }

    public BigDecimal getHarvestAmount() { return harvestAmount; }

    public CropUsePartCategory getMedicinalPart() { return medicinalPart; }

    public HarvestSource getHarvestSource() { return harvestSource; }

    public int getGrowthPeriod() { return growthPeriod; }

    public boolean isLastHarvest() { return isLastHarvest; }
}
