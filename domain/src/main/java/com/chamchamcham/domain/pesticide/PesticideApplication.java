package com.chamchamcham.domain.pesticide;

import com.chamchamcham.domain.common.BaseTimeEntity;
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
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "pesticide_application",
    uniqueConstraints = {
    @UniqueConstraint(
            name = "uk_pesticide_application_natural_key",
            columnNames = {"pesticide_id", "pest_id", "crop_name"}
        )
    }
)
public class PesticideApplication extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pesticide_id", nullable = false)
    private Pesticide pesticide;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pest_id", nullable = false)
    private Pest pest;

    @Column(name = "crop_name", nullable = false, length = 128)
    private String cropName;

    @Column(name = "dilution_rate", length = 64)
    private String dilutionRate;

    @Column(name = "usage_amount", length = 64)
    private String usageAmount;

    @Column(name = "usage_timing", length = 64)
    private String usageTiming;

    @Column(name = "max_usage_count", length = 32)
    private String maxUsageCount;

    protected PesticideApplication() {
    }

    public PesticideApplication(
        Pesticide pesticide,
        Pest pest,
        String cropName
    ) {
        this(null, pesticide, pest, cropName, null, null, null, null);
    }

    public PesticideApplication(
        @Nullable UUID id,
        Pesticide pesticide,
        Pest pest,
        String cropName
    ) {
        this(id, pesticide, pest, cropName, null, null, null, null);
    }

    public PesticideApplication(
        @Nullable UUID id,
        Pesticide pesticide,
        Pest pest,
        String cropName,
        @Nullable String dilutionRate,
        @Nullable String usageAmount,
        @Nullable String usageTiming,
        @Nullable String maxUsageCount
    ) {
        this.id = id;
        this.pesticide = Objects.requireNonNull(pesticide, "pesticide");
        this.pest = Objects.requireNonNull(pest, "pest");
        this.cropName = Objects.requireNonNull(cropName, "cropName");
        this.dilutionRate = dilutionRate;
        this.usageAmount = usageAmount;
        this.usageTiming = usageTiming;
        this.maxUsageCount = maxUsageCount;
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Pesticide getPesticide() {
        return pesticide;
    }

    public Pest getPest() {
        return pest;
    }

    public String getCropName() {
        return cropName;
    }

    @Nullable
    public String getDilutionRate() {
        return dilutionRate;
    }

    public void setDilutionRate(@Nullable String dilutionRate) {
        this.dilutionRate = dilutionRate;
    }

    @Nullable
    public String getUsageAmount() {
        return usageAmount;
    }

    public void setUsageAmount(@Nullable String usageAmount) {
        this.usageAmount = usageAmount;
    }

    @Nullable
    public String getUsageTiming() {
        return usageTiming;
    }

    public void setUsageTiming(@Nullable String usageTiming) {
        this.usageTiming = usageTiming;
    }

    @Nullable
    public String getMaxUsageCount() {
        return maxUsageCount;
    }

    public void setMaxUsageCount(@Nullable String maxUsageCount) {
        this.maxUsageCount = maxUsageCount;
    }

    public void updateUsageDetails(@Nullable String dilutionRate, @Nullable String usageAmount,
        @Nullable String usageTiming, @Nullable String maxUsageCount) {
        this.dilutionRate = dilutionRate;
        this.usageAmount = usageAmount;
        this.usageTiming = usageTiming;
        this.maxUsageCount = maxUsageCount;
    }
}
