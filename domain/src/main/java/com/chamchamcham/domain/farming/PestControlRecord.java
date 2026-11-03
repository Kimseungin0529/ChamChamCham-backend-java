package com.chamchamcham.domain.farming;

import java.util.Objects;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.Pesticide;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pest_control_record")
public class PestControlRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FarmingRecord record;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pesticide_id", nullable = false)
    private Pesticide pesticide;

    @Column(name = "pesticide_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal pesticideAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "pesticide_amount_unit", nullable = false, length = 16)
    private PesticideAmountUnit pesticideAmountUnit;

    @Column(name = "total_spray_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalSprayAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "total_spray_amount_unit", nullable = false, length = 16)
    private SprayAmountUnit totalSprayAmountUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pest_id")
    private Pest pest;

    protected PestControlRecord() {}

    public PestControlRecord(UUID id, FarmingRecord record, Pesticide pesticide, BigDecimal pesticideAmount, PesticideAmountUnit pesticideAmountUnit, BigDecimal totalSprayAmount, SprayAmountUnit totalSprayAmountUnit, Pest pest) {
        this.id = id;
        this.record = Objects.requireNonNull(record, "record");
        this.pesticide = Objects.requireNonNull(pesticide, "pesticide");
        this.pesticideAmount = Objects.requireNonNull(pesticideAmount, "pesticideAmount");
        this.pesticideAmountUnit = Objects.requireNonNull(pesticideAmountUnit, "pesticideAmountUnit");
        this.totalSprayAmount = Objects.requireNonNull(totalSprayAmount, "totalSprayAmount");
        this.totalSprayAmountUnit = Objects.requireNonNull(totalSprayAmountUnit, "totalSprayAmountUnit");
        this.pest = pest;
    }

    public PestControlRecord(FarmingRecord record, Pesticide pesticide, BigDecimal pesticideAmount, PesticideAmountUnit pesticideAmountUnit, BigDecimal totalSprayAmount, SprayAmountUnit totalSprayAmountUnit) {
        this(null, record, pesticide, pesticideAmount, pesticideAmountUnit, totalSprayAmount, totalSprayAmountUnit, null);
    }

    public PestControlRecord(FarmingRecord record, Pesticide pesticide, BigDecimal pesticideAmount, PesticideAmountUnit pesticideAmountUnit, BigDecimal totalSprayAmount, SprayAmountUnit totalSprayAmountUnit, Pest pest) {
        this(null, record, pesticide, pesticideAmount, pesticideAmountUnit, totalSprayAmount, totalSprayAmountUnit, pest);
    }

    public UUID getId() { return id; }

    public FarmingRecord getRecord() { return record; }

    public Pesticide getPesticide() { return pesticide; }

    public BigDecimal getPesticideAmount() { return pesticideAmount; }

    public PesticideAmountUnit getPesticideAmountUnit() { return pesticideAmountUnit; }

    public BigDecimal getTotalSprayAmount() { return totalSprayAmount; }

    public SprayAmountUnit getTotalSprayAmountUnit() { return totalSprayAmountUnit; }

    public Pest getPest() { return pest; }
}
